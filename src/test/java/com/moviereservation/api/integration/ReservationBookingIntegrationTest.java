package com.moviereservation.api.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviereservation.api.domain.entities.*;
import com.moviereservation.api.domain.enums.*;
import com.moviereservation.api.repository.*;

/**
 * Integration tests for the reservation booking flow.
 * Tests the complete flow from user authentication to seat booking using Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Reservation Booking Flow Integration Tests")
class ReservationBookingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "false"); // Disable Flyway for tests
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Movie testMovie;
    private Showtime testShowtime;
    private List<SeatInstance> testSeats;
    private String jwtToken;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        // Clean up
        reservationRepository.deleteAll();
        seatInstanceRepository.deleteAll();
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();
        seatTemplateRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPhone("+1234567890");
        testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser.setRole(UserRole.CUSTOMER);
        testUser = userRepository.save(testUser);

        // Login to get JWT token
        String loginJson = """
                {
                    "email": "test@example.com",
                    "password": "Password123!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        jwtToken = loginResponse.get("data").get("accessToken").asText();

        // Create test movie
        testMovie = new Movie();
        testMovie.setTitle("Test Movie");
        testMovie.setDescription("Test Description");
        testMovie.setDuration(120);
        testMovie.setGenre(Genre.ACTION);
        testMovie.setStatus(MovieStatus.ACTIVE);
        testMovie.setRating("PG-13");
        testMovie = movieRepository.save(testMovie);

        // Create seat templates
        for (char row = 'A'; row <= 'J'; row++) {
            for (short seatNum = 1; seatNum <= 12; seatNum++) {
                SeatTemplate template = new SeatTemplate();
                template.setScreenNumber((short) 1);
                template.setRowLabel(row);
                template.setSeatNumber(seatNum);
                template.setType(row >= 'H' ? SeatType.PREMIUM : SeatType.REGULAR);
                template.setBasePrice(row >= 'H' ? new BigDecimal("15.00") : new BigDecimal("10.00"));
                seatTemplateRepository.save(template);
            }
        }

        // Create test showtime
        testShowtime = new Showtime();
        testShowtime.setMovie(testMovie);
        testShowtime.setStartTime(Instant.now().plus(3, ChronoUnit.HOURS));
        testShowtime.setEndTime(Instant.now().plus(5, ChronoUnit.HOURS));
        testShowtime.setScreenNumber((short) 1);
        testShowtime.setBasePrice(new BigDecimal("10.00"));
        testShowtime.setStatus(ShowtimeStatus.SCHEDULED);
        testShowtime.setAvailableSeatsCount((short) 120);
        testShowtime = showtimeRepository.save(testShowtime);

        // Create seat instances for showtime
        testSeats = seatTemplateRepository.findByScreenNumber((short) 1)
                .stream()
                .map(template -> {
                    SeatInstance seat = new SeatInstance();
                    seat.setShowtime(testShowtime);
                    seat.setSeatTemplate(template);
                    seat.setRowLabel(template.getRowLabel());
                    seat.setSeatNumber(template.getSeatNumber());
                    seat.setType(template.getType());
                    seat.setPrice(template.getType() == SeatType.PREMIUM
                            ? testShowtime.getBasePrice().multiply(new BigDecimal("1.5"))
                            : testShowtime.getBasePrice());
                    seat.setStatus(SeatStatus.AVAILABLE);
                    return seatInstanceRepository.save(seat);
                })
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("Should complete full booking flow successfully")
    void shouldCompleteFullBookingFlow() throws Exception {
        // Step 1: Get seat map
        mockMvc.perform(get("/api/showtimes/" + testShowtime.getId() + "/seats")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats").isArray())
                .andExpect(jsonPath("$.data.totalSeats").value(120));

        // Step 2: Create reservation with 3 seats
        List<UUID> seatIds = testSeats.stream()
                .limit(3)
                .map(SeatInstance::getId)
                .toList();

        String createReservationJson = String.format("""
                {
                    "showtimeId": "%s",
                    "seatInstanceIds": ["%s", "%s", "%s"]
                }
                """, testShowtime.getId(), seatIds.get(0), seatIds.get(1), seatIds.get(2));

        MvcResult reservationResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReservationJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.bookingReference").exists())
                .andExpect(jsonPath("$.data.totalPrice").value(30.00))
                .andReturn();

        JsonNode reservationResponse = objectMapper.readTree(reservationResult.getResponse().getContentAsString());
        String bookingReference = reservationResponse.get("data").get("bookingReference").asText();

        // Step 3: Verify reservation was created
        mockMvc.perform(get("/api/reservations/" + bookingReference)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").value(bookingReference))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.seats").isArray())
                .andExpect(jsonPath("$.data.seats.length()").value(3));

        // Step 4: Verify seats are held
        mockMvc.perform(get("/api/showtimes/" + testShowtime.getId() + "/seats")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats[?(@.status == 'HELD')]").isArray());
    }

    @Test
    @DisplayName("Should prevent double booking of same seats")
    void shouldPreventDoubleBooking() throws Exception {
        // User 1 books seats
        List<UUID> seatIds = testSeats.stream()
                .limit(2)
                .map(SeatInstance::getId)
                .toList();

        String createReservation1 = String.format("""
                {
                    "showtimeId": "%s",
                    "seatInstanceIds": ["%s", "%s"]
                }
                """, testShowtime.getId(), seatIds.get(0), seatIds.get(1));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReservation1))
                .andExpect(status().isCreated());

        // User 2 tries to book same seats - should fail
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReservation1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("Should cancel reservation and release seats")
    void shouldCancelReservationAndReleaseSeats() throws Exception {
        // Create reservation
        List<UUID> seatIds = testSeats.stream()
                .limit(2)
                .map(SeatInstance::getId)
                .toList();

        String createReservationJson = String.format("""
                {
                    "showtimeId": "%s",
                    "seatInstanceIds": ["%s", "%s"]
                }
                """, testShowtime.getId(), seatIds.get(0), seatIds.get(1));

        MvcResult reservationResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReservationJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode reservationResponse = objectMapper.readTree(reservationResult.getResponse().getContentAsString());
        String bookingReference = reservationResponse.get("data").get("bookingReference").asText();

        // Manually confirm the reservation (simulate payment success)
        Reservation reservation = reservationRepository.findByBookingReference(bookingReference).orElseThrow();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        // Mark seats as reserved
        List<SeatInstance> bookedSeats = seatInstanceRepository.findAllById(seatIds);
        bookedSeats.forEach(seat -> seat.setStatus(SeatStatus.RESERVED));
        seatInstanceRepository.saveAll(bookedSeats);

        // Cancel reservation
        mockMvc.perform(post("/api/reservations/" + bookingReference + "/cancel")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Verify seats are available again
        List<SeatInstance> seats = seatInstanceRepository.findAllById(seatIds);
        assertThat(seats).allMatch(seat -> seat.getStatus() == SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should reject booking more than 10 seats")
    void shouldRejectBookingMoreThan10Seats() throws Exception {
        // Try to book 11 seats
        List<UUID> seatIds = testSeats.stream()
                .limit(11)
                .map(SeatInstance::getId)
                .toList();

        String createReservationJson = String.format("""
                {
                    "showtimeId": "%s",
                    "seatInstanceIds": %s
                }
                """, testShowtime.getId(), objectMapper.writeValueAsString(seatIds));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReservationJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("10 seats")));
    }
}
