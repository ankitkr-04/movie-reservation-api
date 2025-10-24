CREATE TABLE email_logs (
    email_log_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
     -- Email metadata
    recipient_email VARCHAR(255) NOT NULL,
    email_type VARCHAR(50) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    
    -- Delivery status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    error_message TEXT,
    retry_count SMALLINT NOT NULL DEFAULT 0,
    
    -- Related entities
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    reservation_id UUID REFERENCES reservations(reservation_id) ON DELETE SET NULL,
    
    -- Email content (for debugging/resending)
    email_body TEXT,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

       CONSTRAINT chk_email_logs_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT chk_email_logs_type CHECK (email_type IN (
        'BOOKING_CONFIRMATION',
        'CANCELLATION_CONFIRMATION',
        'REFUND_NOTIFICATION',
        'PAYMENT_FAILURE',
        'SHOWTIME_CANCELLED'
    )),
    CONSTRAINT chk_email_logs_retry CHECK (retry_count >= 0)
);


CREATE MATERIALIZED VIEW analytics_daily_revenue AS 
SELECT
    DATE(r.created_at) AS date,
    COUNT(DISTINCT r.reservation_id) AS total_bookings,
    COUNT(DISTINCT r.user_id) AS unique_customers,
    SUM(r.total_price) AS total_revenue,
    AVG(r.total_price) AS avg_booking_value,
    COUNT(rs.seat_instance_id) AS total_seats_sold
FROM reservations r
INNER JOIN reservation_seats rs ON r.reservation_id = rs.reservation_id
WHERE r.status = 'CONFIRMED'
GROUP BY DATE(r.created_at);

CREATE UNIQUE INDEX idx_analytics_daily_revenue_date ON analytics_daily_revenue (date DESC);

COMMENT ON MATERIALIZED VIEW analytics_daily_revenue IS 'Daily revenue aggregations for confirmed reservations';


-- ============================================================================
-- Analytics Materialized View: Movie Performance
-- Aggregates booking metrics per movie
-- ============================================================================
CREATE MATERIALIZED VIEW analytics_movie_performance AS
SELECT
    m.movie_id,
    m.title,
    m.genre,
    COUNT(DISTINCT r.reservation_id) AS total_bookings,
    COUNT(rs.seat_instance_id) AS total_seats_sold,
    SUM(r.total_price) AS total_revenue,
    AVG(r.total_price) AS avg_booking_value,
    COUNT(DISTINCT r.user_id) AS unique_customers,
    MIN(r.created_at) AS first_booking_date,
    MAX(r.created_at) AS last_booking_date
FROM movies m
INNER JOIN showtimes st ON m.movie_id = st.movie_id
INNER JOIN reservations r ON st.showtime_id = r.showtime_id
INNER JOIN reservation_seats rs ON r.reservation_id = rs.reservation_id
WHERE r.status = 'CONFIRMED'
GROUP BY m.movie_id, m.title, m.genre;

-- Index for sorting by revenue/popularity
CREATE UNIQUE INDEX idx_analytics_movie_perf_id ON analytics_movie_performance (movie_id);
CREATE INDEX idx_analytics_movie_perf_revenue ON analytics_movie_performance (total_revenue DESC);
CREATE INDEX idx_analytics_movie_perf_bookings ON analytics_movie_performance (total_bookings DESC);

COMMENT ON MATERIALIZED VIEW analytics_movie_performance IS 'Booking and revenue metrics per movie';

-- ============================================================================
-- Analytics Materialized View: Showtime Occupancy
-- Calculates occupancy rates for each showtime
-- ============================================================================
CREATE MATERIALIZED VIEW analytics_showtime_occupancy AS
SELECT
    st.showtime_id,
    m.title AS movie_title,
    st.screen_number,
    st.start_time,
    st.base_price,
    120 AS total_seats, -- Fixed capacity per screen
    COUNT(si.seat_instance_id) FILTER (WHERE si.status = 'RESERVED') AS seats_sold,
    ROUND(
        (COUNT(si.seat_instance_id) FILTER (WHERE si.status = 'RESERVED')::NUMERIC / 120) * 100,
        2
    ) AS occupancy_rate_percent,
    SUM(r.total_price) AS total_revenue,
    COUNT(DISTINCT r.reservation_id) AS total_bookings
FROM showtimes st
INNER JOIN movies m ON st.movie_id = m.movie_id
LEFT JOIN seat_instance si ON st.showtime_id = si.showtime_id
LEFT JOIN reservation_seats rs ON si.seat_instance_id = rs.seat_instance_id
LEFT JOIN reservations r ON rs.reservation_id = r.reservation_id AND r.status = 'CONFIRMED'
WHERE st.deleted_at IS NULL
GROUP BY st.showtime_id, m.title, st.screen_number, st.start_time, st.base_price;

-- Indexes for filtering and sorting
CREATE UNIQUE INDEX idx_analytics_showtime_occ_id ON analytics_showtime_occupancy (showtime_id);
CREATE INDEX idx_analytics_showtime_occ_time ON analytics_showtime_occupancy (start_time DESC);
CREATE INDEX idx_analytics_showtime_occ_rate ON analytics_showtime_occupancy (occupancy_rate_percent DESC);

COMMENT ON MATERIALIZED VIEW analytics_showtime_occupancy IS 'Occupancy rates and revenue per showtime';

-- ============================================================================
-- Function: Refresh All Analytics Materialized Views
-- Call this after bulk operations or via scheduled job
-- ============================================================================
CREATE OR REPLACE FUNCTION refresh_analytics_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY analytics_daily_revenue;
    REFRESH MATERIALIZED VIEW CONCURRENTLY analytics_movie_performance;
    REFRESH MATERIALIZED VIEW CONCURRENTLY analytics_showtime_occupancy;
    
    RAISE NOTICE 'All analytics materialized views refreshed successfully';
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_analytics_views() IS 'Refreshes all analytics materialized views. Run after bulk data changes or daily via cron.';

-- ============================================================================
-- Function: Get Revenue by Date Range
-- Returns aggregated revenue metrics for a date range
-- ============================================================================
CREATE OR REPLACE FUNCTION get_revenue_by_date_range(
    p_start_date DATE,
    p_end_date DATE
)
RETURNS TABLE (
    period_start DATE,
    period_end DATE,
    total_bookings BIGINT,
    total_revenue NUMERIC,
    avg_booking_value NUMERIC,
    total_seats_sold BIGINT,
    unique_customers BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p_start_date AS period_start,
        p_end_date AS period_end,
        SUM(adr.total_bookings)::BIGINT AS total_bookings,
        SUM(adr.total_revenue) AS total_revenue,
        AVG(adr.avg_booking_value) AS avg_booking_value,
        SUM(adr.total_seats_sold)::BIGINT AS total_seats_sold,
        SUM(adr.unique_customers)::BIGINT AS unique_customers
    FROM analytics_daily_revenue adr
    WHERE adr.date BETWEEN p_start_date AND p_end_date;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_revenue_by_date_range IS 'Returns aggregated revenue metrics for a specific date range';

-- ============================================================================
-- Function: Get Peak Booking Hours
-- Analyzes booking patterns by hour of day
-- ============================================================================
CREATE OR REPLACE FUNCTION get_peak_booking_hours()
RETURNS TABLE (
    hour_of_day INTEGER,
    total_bookings BIGINT,
    total_revenue NUMERIC,
    avg_seats_per_booking NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        EXTRACT(HOUR FROM r.created_at)::INTEGER AS hour_of_day,
        COUNT(r.reservation_id)::BIGINT AS total_bookings,
        SUM(r.total_price) AS total_revenue,
        AVG(
            (SELECT COUNT(*) FROM reservation_seats rs2 WHERE rs2.reservation_id = r.reservation_id)
        ) AS avg_seats_per_booking
    FROM reservations r
    WHERE r.status = 'CONFIRMED'
    GROUP BY EXTRACT(HOUR FROM r.created_at)
    ORDER BY total_bookings DESC;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_peak_booking_hours IS 'Returns booking patterns by hour of day (0-23)';

-- ============================================================================
-- Function: Get Top Movies by Revenue
-- Returns most profitable movies
-- ============================================================================
CREATE OR REPLACE FUNCTION get_top_movies_by_revenue(p_limit INTEGER DEFAULT 10)
RETURNS TABLE (
    movie_id UUID,
    title VARCHAR,
    genre VARCHAR,
    total_revenue NUMERIC,
    total_bookings BIGINT,
    total_seats_sold BIGINT,
    occupancy_rate NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        amp.movie_id,
        amp.title,
        amp.genre,
        amp.total_revenue,
        amp.total_bookings,
        amp.total_seats_sold,
        ROUND((amp.total_seats_sold::NUMERIC / NULLIF(
            (SELECT COUNT(*) FROM showtimes st2 WHERE st2.movie_id = amp.movie_id) * 120,
            0
        )) * 100, 2) AS occupancy_rate
    FROM analytics_movie_performance amp
    ORDER BY amp.total_revenue DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_top_movies_by_revenue IS 'Returns top N movies by revenue with occupancy rate';

-- ============================================================================
-- Trigger: Update Email Log Timestamp on Status Change
-- ============================================================================
CREATE OR REPLACE FUNCTION update_email_log_timestamps()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    
    IF NEW.status = 'SENT' AND OLD.status != 'SENT' THEN
        NEW.sent_at = CURRENT_TIMESTAMP;
    END IF;
    
    IF NEW.status = 'FAILED' AND OLD.status != 'FAILED' THEN
        NEW.failed_at = CURRENT_TIMESTAMP;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_email_logs_timestamps
    BEFORE UPDATE ON email_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_email_log_timestamps();

COMMENT ON TRIGGER trg_email_logs_timestamps ON email_logs IS 'Auto-updates sent_at/failed_at timestamps when status changes';

-- ============================================================================
-- Initial Analytics Refresh
-- Populate materialized views with existing data
-- ============================================================================
SELECT refresh_analytics_views();