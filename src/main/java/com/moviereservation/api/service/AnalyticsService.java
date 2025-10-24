package com.moviereservation.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for analytics and reporting.
 * Uses PostgreSQL functions and materialized views for performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Refresh all analytics materialized views.
     * Should be called after bulk operations or daily via scheduled task.
     */
    @Transactional
    public void refreshAnalyticsViews() {
        log.info("Refreshing analytics materialized views");

        jdbcTemplate.execute("SELECT refresh_analytics_views()");

        log.info("Analytics views refreshed successfully");
    }

    /**
     * Get revenue metrics for a date range.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueByDateRange(final LocalDate startDate, final LocalDate endDate) {
        log.debug("Fetching revenue for period: {} to {}", startDate, endDate);

        final String sql = "SELECT * FROM get_revenue_by_date_range(?, ?)";

        return jdbcTemplate.queryForMap(sql, startDate, endDate);
    }

    /**
     * Get peak booking hours analysis.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPeakBookingHours() {
        log.debug("Fetching peak booking hours");

        final String sql = "SELECT * FROM get_peak_booking_hours()";

        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get top movies by revenue.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopMoviesByRevenue(final int limit) {
        log.debug("Fetching top {} movies by revenue", limit);

        final String sql = "SELECT * FROM get_top_movies_by_revenue(?)";

        return jdbcTemplate.queryForList(sql, limit);
    }

    /**
     * Get daily revenue statistics from materialized view.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyRevenue(final LocalDate startDate, final LocalDate endDate) {
        log.debug("Fetching daily revenue: {} to {}", startDate, endDate);

        final String sql = """
                SELECT
                    date,
                    total_bookings,
                    unique_customers,
                    total_revenue,
                    avg_booking_value,
                    total_seats_sold
                FROM analytics_daily_revenue
                WHERE date BETWEEN ? AND ?
                ORDER BY date DESC
                """;

        return jdbcTemplate.queryForList(sql, startDate, endDate);
    }

    /**
     * Get movie performance metrics.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMoviePerformance() {
        log.debug("Fetching movie performance metrics");

        final String sql = """
                SELECT
                    movie_id,
                    title,
                    genre,
                    total_bookings,
                    total_seats_sold,
                    total_revenue,
                    avg_booking_value,
                    unique_customers
                FROM analytics_movie_performance
                ORDER BY total_revenue DESC
                """;

        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get showtime occupancy rates.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getShowtimeOccupancy(final LocalDate date) {
        log.debug("Fetching showtime occupancy for date: {}", date);

        final String sql = """
                SELECT
                    showtime_id,
                    movie_title,
                    screen_number,
                    start_time,
                    total_seats,
                    seats_sold,
                    occupancy_rate_percent,
                    total_revenue,
                    total_bookings
                FROM analytics_showtime_occupancy
                WHERE DATE(start_time) = ?
                ORDER BY start_time
                """;

        return jdbcTemplate.queryForList(sql, date);
    }

    /**
     * Get revenue summary statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueSummary() {
        log.debug("Fetching revenue summary");

        final String sql = """
                SELECT
                    SUM(total_revenue) as total_revenue,
                    SUM(total_bookings) as total_bookings,
                    AVG(avg_booking_value) as avg_booking_value,
                    SUM(total_seats_sold) as total_seats_sold,
                    SUM(unique_customers) as total_unique_customers
                FROM analytics_daily_revenue
                """;

        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * Get occupancy rate statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOccupancyStats() {
        log.debug("Fetching occupancy statistics");

        final String sql = """
                SELECT
                    AVG(occupancy_rate_percent) as avg_occupancy_rate,
                    MIN(occupancy_rate_percent) as min_occupancy_rate,
                    MAX(occupancy_rate_percent) as max_occupancy_rate,
                    COUNT(*) FILTER (WHERE occupancy_rate_percent >= 80) as high_occupancy_count,
                    COUNT(*) as total_showtimes
                FROM analytics_showtime_occupancy
                """;

        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * Get revenue by genre.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenueByGenre() {
        log.debug("Fetching revenue by genre");

        final String sql = """
                SELECT
                    genre,
                    SUM(total_revenue) as total_revenue,
                    SUM(total_bookings) as total_bookings,
                    SUM(total_seats_sold) as total_seats_sold
                FROM analytics_movie_performance
                GROUP BY genre
                ORDER BY total_revenue DESC
                """;

        return jdbcTemplate.queryForList(sql);
    }
}