package com.moviereservation.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moviereservation.api.domain.entities.*;
import com.moviereservation.api.domain.enums.*;
import com.moviereservation.api.exception.*;
import com.moviereservation.api.repository.*;
import com.moviereservation.api.web.dto.request.reservation.CreateReservationRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Tests")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatInstanceRepository seatInstanceRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private Showtime testShowtime;
    private Movie testMovie;
    private List<SeatInstance> testSeats;
    private CreateReservationRequest createReservationRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.CUSTOMER);

        // Setup test movie
        testMovie = new Movie();
        testMovie.setId(UUID.randomUUID());
        testMovie.setTitle("Test Movie");
        testMovie.setDuration(120);
        testMovie.setGenre(Genre.ACTION);
        testMovie.setStatus(MovieStatus.ACTIVE);

        // Setup test showtime (2 hours in future)
        testShowtime = new Showtime();
        testShowtime.setId(UUID.randomUUID());
        testShowtime.setMovie(testMovie);
        testShowtime.setStartTime(Instant.now().plus(2, ChronoUnit.HOURS));
        testShowtime.setEndTime(Instant.now().plus(4, ChronoUnit.HOURS));
        testShowtime.setScreenNumber((short) 1);
        testShowtime.setBasePrice(new BigDecimal("10.00"));
        testShowtime.setStatus(ShowtimeStatus.SCHEDULED);
        testShowtime.setAvailableSeatsCount((short) 120);

        // Setup test seats
        testSeats = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SeatInstance seat = new SeatInstance();
            seat.setId(UUID.randomUUID());
            seat.setShowtime(testShowtime);
            seat.setRowLabel('A');
            seat.setSeatNumber((short) (i + 1));
            seat.setType(SeatType.REGULAR);
            seat.setPrice(new BigDecimal("10.00"));
            seat.setStatus(SeatStatus.AVAILABLE);
            testSeats.add(seat);
        }

        // Setup request
        List<UUID> seatIds = testSeats.stream().map(SeatInstance::getId).toList();
        createReservationRequest = CreateReservationRequest.builder()
                .showtimeId(testShowtime.getId())
                .seatInstanceIds(seatIds)
                .build();
    }

    @Test
    @DisplayName("Should create reservation successfully with available seats")
    void shouldCreateReservationSuccessfully() {
        // Arrange
        when(userService.findById(testUser.getId())).thenReturn(testUser);
        when(showtimeRepository.findById(any(UUID.class))).thenReturn(Optional.of(testShowtime));
        when(seatInstanceRepository.findAllByIdWithLock(anyList())).thenReturn(testSeats);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(UUID.randomUUID());
            reservation.setBookingReference("ABC12345");
            return reservation;
        });

        // Act
        Reservation result = reservationService.create(testUser.getId(), createReservationRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(result.getTotalPrice()).isEqualTo(new BigDecimal("30.00")); // 3 seats × $10

        verify(seatInstanceRepository).findAllByIdWithLock(anyList());
        verify(reservationRepository).save(any(Reservation.class));

        // Verify seats are marked as HELD
        testSeats.forEach(seat -> assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD));
    }

    @Test
    @DisplayName("Should throw exception when booking more than 10 seats")
    void shouldThrowExceptionWhenBookingTooManySeats() {
        // Arrange
        List<UUID> tooManySeats = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tooManySeats.add(UUID.randomUUID());
        }
        CreateReservationRequest invalidRequest = CreateReservationRequest.builder()
                .showtimeId(testShowtime.getId())
                .seatInstanceIds(tooManySeats)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> reservationService.create(testUser.getId(), invalidRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("10 seats");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when seat is already held")
    void shouldThrowExceptionWhenSeatAlreadyHeld() {
        // Arrange
        testSeats.get(0).setStatus(SeatStatus.HELD);
        testSeats.get(0).setHeldAt(Instant.now().minus(2, ChronoUnit.MINUTES));

        when(userService.findById(testUser.getId())).thenReturn(testUser);
        when(showtimeRepository.findById(any(UUID.class))).thenReturn(Optional.of(testShowtime));
        when(seatInstanceRepository.findAllByIdWithLock(anyList())).thenReturn(testSeats);

        // Act & Assert
        assertThatThrownBy(() -> reservationService.create(testUser.getId(), createReservationRequest))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("unavailable");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when seat is reserved")
    void shouldThrowExceptionWhenSeatReserved() {
        // Arrange
        testSeats.get(0).setStatus(SeatStatus.RESERVED);

        when(userService.findById(testUser.getId())).thenReturn(testUser);
        when(showtimeRepository.findById(any(UUID.class))).thenReturn(Optional.of(testShowtime));
        when(seatInstanceRepository.findAllByIdWithLock(anyList())).thenReturn(testSeats);

        // Act & Assert
        assertThatThrownBy(() -> reservationService.create(testUser.getId(), createReservationRequest))
                .isInstanceOf(SeatUnavailableException.class);

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when showtime is in the past")
    void shouldThrowExceptionWhenShowtimeInPast() {
        // Arrange
        testShowtime.setStartTime(Instant.now().minus(2, ChronoUnit.HOURS));
        testShowtime.setStatus(ShowtimeStatus.COMPLETED);

        when(userService.findById(testUser.getId())).thenReturn(testUser);
        when(showtimeRepository.findById(any(UUID.class))).thenReturn(Optional.of(testShowtime));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.create(testUser.getId(), createReservationRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("past");

        verify(seatInstanceRepository, never()).findAllByIdWithLock(anyList());
    }

    @Test
    @DisplayName("Should throw exception when showtime is cancelled")
    void shouldThrowExceptionWhenShowtimeCancelled() {
        // Arrange
        testShowtime.setStatus(ShowtimeStatus.CANCELLED);

        when(userService.findById(testUser.getId())).thenReturn(testUser);
        when(showtimeRepository.findById(any(UUID.class))).thenReturn(Optional.of(testShowtime));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.create(testUser.getId(), createReservationRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("CANCELLED");

        verify(seatInstanceRepository, never()).findAllByIdWithLock(anyList());
    }

    @Test
    @DisplayName("Should cancel reservation successfully")
    void shouldCancelReservationSuccessfully() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setBookingReference("ABC12345");
        reservation.setUser(testUser);
        reservation.setShowtime(testShowtime);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setTotalPrice(new BigDecimal("30.00"));
        reservation.setCreatedAt(Instant.now());

        List<ReservationSeat> reservationSeats = new ArrayList<>();
        for (SeatInstance seat : testSeats) {
            ReservationSeat rs = new ReservationSeat();
            rs.setSeatInstance(seat);
            rs.setReservation(reservation);
            reservationSeats.add(rs);
        }
        reservation.setReservationSeats(reservationSeats);

        when(reservationRepository.findByBookingReference("ABC12345"))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Act
        Reservation result = reservationService.cancel("ABC12345", testUser.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling within 2 hours of showtime")
    void shouldThrowExceptionWhenCancellingTooLate() {
        // Arrange
        testShowtime.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS)); // Only 1 hour away

        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setBookingReference("ABC12345");
        reservation.setUser(testUser);
        reservation.setShowtime(testShowtime);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(Instant.now());

        when(reservationRepository.findByBookingReference("ABC12345"))
                .thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.cancel("ABC12345", testUser.getId()))
                .isInstanceOf(InvalidReservationCancellationException.class)
                .hasMessageContaining("2 hours");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should find reservation by booking reference")
    void shouldFindReservationByBookingReference() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setBookingReference("ABC12345");
        reservation.setUser(testUser);
        reservation.setShowtime(testShowtime);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findByBookingReference("ABC12345"))
                .thenReturn(Optional.of(reservation));

        // Act
        Optional<Reservation> result = reservationRepository.findByBookingReference("ABC12345");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getBookingReference()).isEqualTo("ABC12345");
        assertThat(result.get().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        verify(reservationRepository).findByBookingReference("ABC12345");
    }
}
