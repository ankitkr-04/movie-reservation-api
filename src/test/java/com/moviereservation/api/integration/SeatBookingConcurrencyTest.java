package com.moviereservation.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.moviereservation.api.domain.entities.*;
import com.moviereservation.api.domain.enums.*;
import com.moviereservation.api.exception.SeatUnavailableException;
import com.moviereservation.api.repository.*;
import com.moviereservation.api.service.ReservationService;
import com.moviereservation.api.web.dto.request.reservation.CreateReservationRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Concurrency stress tests for seat booking.
 * Validates that pessimistic locking prevents double-booking under high
 * concurrency.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Seat Booking Concurrency Stress Tests")
@Slf4j
class SeatBookingConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"));

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatTemplateRepository seatTemplateRepository;

    @Autowired
    private SeatInstanceRepository seatInstanceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private List<User> testUsers;
    private Showtime testShowtime;
    private List<SeatInstance> testSeats;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up
        reservationRepository.deleteAll();
        seatInstanceRepository.deleteAll();
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();
        seatTemplateRepository.deleteAll();

        // Create test users
        testUsers = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final User user = new User();
            user.setEmail("user" + i + "@test.com");
            user.setFullName("Test User " + i);
            user.setPhone("+123456789" + i);
            user.setPasswordHash("hashedPassword");
            user.setRole(UserRole.CUSTOMER);
            testUsers.add(userRepository.save(user));
        }

        // Create test movie
        Movie movie = new Movie();
        movie.setTitle("Concurrency Test Movie");
        movie.setDuration(120);
        movie.setGenre(Genre.ACTION);
        movie.setStatus(MovieStatus.ACTIVE);
        movie = movieRepository.save(movie);

        // Create seat templates
        for (char row = 'A'; row <= 'J'; row++) {
            for (short seatNum = 1; seatNum <= 12; seatNum++) {
                final SeatTemplate template = new SeatTemplate();
                template.setScreenNumber((short) 1);
                template.setRowLabel(row);
                template.setSeatNumber(seatNum);
                template.setType(row >= 'H' ? SeatType.PREMIUM : SeatType.REGULAR);
                template.setBasePrice(new BigDecimal("10.00"));
                seatTemplateRepository.save(template);
            }
        }

        // Create showtime
        testShowtime = new Showtime();
        testShowtime.setMovie(movie);
        testShowtime.setStartTime(Instant.now().plus(3, ChronoUnit.HOURS));
        testShowtime.setEndTime(Instant.now().plus(5, ChronoUnit.HOURS));
        testShowtime.setScreenNumber((short) 1);
        testShowtime.setBasePrice(new BigDecimal("10.00"));
        testShowtime.setStatus(ShowtimeStatus.SCHEDULED);
        testShowtime.setAvailableSeatsCount((short) 120);
        testShowtime = showtimeRepository.save(testShowtime);

        // Create seat instances
        testSeats = seatTemplateRepository.findByScreenNumber((short) 1)
                .stream()
                .map(template -> {
                    final SeatInstance seat = new SeatInstance();
                    seat.setShowtime(testShowtime);
                    seat.setSeatTemplate(template);
                    seat.setRowLabel(template.getRowLabel());
                    seat.setSeatNumber(template.getSeatNumber());
                    seat.setType(template.getType());
                    seat.setPrice(new BigDecimal("10.00"));
                    seat.setStatus(SeatStatus.AVAILABLE);
                    return seatInstanceRepository.save(seat);
                })
                .toList();
    }

    @Test
    @DisplayName("Should handle 20 concurrent users booking same seats - only 1 succeeds")
    void shouldHandleConcurrentBookingsWithPessimisticLocking() throws InterruptedException {
        // Setup: Pick 3 seats that all users will try to book
        final List<UUID> contestedSeatIds = testSeats.stream()
                .limit(3)
                .map(SeatInstance::getId)
                .toList();

        final int numberOfThreads = 20;
        final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // Create booking tasks
        for (int i = 0; i < numberOfThreads; i++) {
            final User user = testUsers.get(i);

            executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Attempt to book the same seats
                    final CreateReservationRequest request = CreateReservationRequest.builder()
                            .showtimeId(testShowtime.getId())
                            .seatInstanceIds(contestedSeatIds)
                            .build();

                    reservationService.create(user.getId(), request);

                    successCount.incrementAndGet();
                    log.info("User {} successfully booked seats", user.getEmail());

                } catch (SeatUnavailableException | IllegalArgumentException e) {
                    failureCount.incrementAndGet();
                    log.debug("User {} failed to book seats: {}", user.getEmail(), e.getMessage());
                } catch (final Exception e) {
                    exceptions.add(e);
                    log.error("User {} encountered unexpected error", user.getEmail(), e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete (with timeout)
        final boolean completed = completeLatch.await(30, TimeUnit.SECONDS);

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Assertions
        assertThat(completed).as("Not all threads completed within timeout").isTrue();
        assertThat(exceptions)
                .as("Unexpected exceptions occurred: %s", exceptions)
                .isEmpty();

        // CRITICAL: Only 1 booking should succeed, all others must fail
        assertThat(successCount.get())
                .as("Expected exactly 1 successful booking but got %d", successCount.get())
                .isEqualTo(1);
        assertThat(failureCount.get())
                .as("Expected 19 failed bookings but got %d", failureCount.get())
                .isEqualTo(19);

        // Verify database state
        final List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);

        final List<SeatInstance> seats = seatInstanceRepository.findAllById(contestedSeatIds);
        assertThat(seats).allMatch(seat -> seat.getStatus() == SeatStatus.HELD);

        log.info("✅ Concurrency test passed: {} success, {} failures", successCount.get(), failureCount.get());
    }

    @Test
    @DisplayName("Should handle 50 users booking different seats simultaneously")
    void shouldHandleHighConcurrencyForDifferentSeats() throws InterruptedException {
        final int numberOfThreads = 50;
        final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        // Each user books different seats
        for (int i = 0; i < numberOfThreads; i++) {
            final User user = testUsers.get(i % testUsers.size());
            final int offset = i * 2; // Each user gets 2 different seats

            executorService.submit(() -> {
                try {
                    startLatch.await();

                    final List<UUID> seatIds = testSeats.stream()
                            .skip(offset)
                            .limit(2)
                            .map(SeatInstance::getId)
                            .toList();

                    final CreateReservationRequest request = CreateReservationRequest.builder()
                            .showtimeId(testShowtime.getId())
                            .seatInstanceIds(seatIds)
                            .build();

                    reservationService.create(user.getId(), request);
                    successCount.incrementAndGet();

                } catch (final Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Booking failed", e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        final boolean completed = completeLatch.await(60, TimeUnit.SECONDS);

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // All bookings should succeed since they're booking different seats
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(numberOfThreads);
        assertThat(failureCount.get()).isZero();

        log.info("✅ High concurrency test passed: {} successful bookings", successCount.get());
    }

    @Test
    @DisplayName("Should maintain data integrity under stress - 100 rapid bookings")
    void shouldMaintainDataIntegrityUnderStress() throws InterruptedException {
        final int numberOfBookings = 100;
        final ExecutorService executorService = Executors.newFixedThreadPool(20);

        final AtomicInteger completedBookings = new AtomicInteger(0);
        final List<Exception> errors = new CopyOnWriteArrayList<>();

        // Submit 100 booking tasks
        IntStream.range(0, numberOfBookings).forEach(i -> {
            executorService.submit(() -> {
                try {
                    final User user = testUsers.get(i % testUsers.size());

                    // Each booking tries to reserve 1 seat
                    final List<UUID> seatIds = List.of(testSeats.get(i).getId());

                    final CreateReservationRequest request = CreateReservationRequest.builder()
                            .showtimeId(testShowtime.getId())
                            .seatInstanceIds(seatIds)
                            .build();

                    reservationService.create(user.getId(), request);
                    completedBookings.incrementAndGet();

                } catch (final SeatUnavailableException e) {
                    // Expected for some attempts
                    log.debug("Seat unavailable (expected): {}", e.getMessage());
                } catch (final Exception e) {
                    errors.add(e);
                    log.error("Unexpected error in stress test", e);
                }
            });
        });

        executorService.shutdown();
        final boolean finished = executorService.awaitTermination(2, TimeUnit.MINUTES);

        assertThat(finished).as("Stress test did not complete in time").isTrue();
        assertThat(errors)
                .as("Unexpected errors occurred: %s", errors)
                .isEmpty();

        // Verify no data corruption
        final List<Reservation> allReservations = reservationRepository.findAll();
        assertThat(allReservations).allMatch(r -> r.getStatus() == ReservationStatus.PENDING_PAYMENT);
        assertThat(allReservations)
                .allMatch(r -> r.getTotalPrice() != null && r.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);
        assertThat(allReservations)
                .allMatch(r -> r.getBookingReference() != null && r.getBookingReference().length() == 8);

        log.info("✅ Stress test passed: {} successful bookings, no data corruption", completedBookings.get());
    }
}
