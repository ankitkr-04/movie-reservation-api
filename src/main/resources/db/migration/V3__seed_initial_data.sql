INSERT INTO seat_template (screen_number, row_label, seat_number, type, base_price)
SELECT
    screen_number,
    row_label,
    seat_number,
    CASE
        WHEN row_label IN ('H','I','J') THEN 'PREMIUM'
        ELSE 'REGULAR'
    END AS seat_type,
    CASE
        WHEN row_label IN ('H','I','J') THEN 15.00::NUMERIC(10,2)
        ELSE 10.00::NUMERIC(10,2)
    END AS base_price
FROM generate_series(1, 5) AS screen(screen_number)
CROSS JOIN (
    VALUES ('A'), ('B'), ('C'), ('D'), ('E'), ('F'), ('G'), ('H'), ('I'), ('J')
) AS rows(row_label)
CROSS JOIN generate_series(1, 12) AS seats(seat_number)
ON CONFLICT (screen_number, row_label, seat_number) DO NOTHING;

WITH admin_user AS (
    INSERT INTO users (full_name, email, phone, role, password_hash)
    VALUES (
        'System Administrator',
        'admin@moviereservation.com',
        '+15555550100',
        'ADMIN',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOgv6yZ9Ec/8P3dC1F6aP/Gh9QP5y1NFe'
    )
    ON CONFLICT (email) DO UPDATE
        SET full_name = EXCLUDED.full_name,
            phone = EXCLUDED.phone,
            role = EXCLUDED.role,
            password_hash = EXCLUDED.password_hash,
            updated_at = CURRENT_TIMESTAMP
    RETURNING user_id
),
movie_seed AS (
    INSERT INTO movies (title, description, duration, genre, release_date, poster_url, rating, status)
    SELECT
        m.title,
        m.description,
        m.duration,
        m.genre,
        m.release_date,
        m.poster_url,
        m.rating,
        m.status
    FROM (
        VALUES
            (
                'The Last Horizon',
                'A retired pilot leads a daring mission to evacuate civilians before a catastrophic solar storm.',
                130,
                'ACTION',
                TIMESTAMPTZ '2025-10-01 00:00:00+00',
                'https://cdn.example.com/posters/last-horizon.jpg',
                'PG-13',
                'ACTIVE'
            ),
            (
                'Symphony of Shadows',
                'An investigative journalist uncovers a conspiracy hidden within a world-renowned orchestra.',
                118,
                'THRILLER',
                TIMESTAMPTZ '2025-09-15 00:00:00+00',
                'https://cdn.example.com/posters/symphony-shadows.jpg',
                'PG',
                'ACTIVE'
            )
    ) AS m(title, description, duration, genre, release_date, poster_url, rating, status)
    ON CONFLICT (title) DO NOTHING
    RETURNING movie_id, title
),
selected_movies AS (
    SELECT movie_id, title FROM movie_seed
    UNION
    SELECT movie_id, title FROM movies WHERE title IN ('The Last Horizon', 'Symphony of Shadows')
),
showtime_seed AS (
    INSERT INTO showtimes (movie_id, start_time, end_time, screen_number, base_price, created_by)
    SELECT
        sm.movie_id,
        s.start_time,
        s.start_time + make_interval(mins => m.duration + 15) AS end_time,
        s.screen_number,
        s.base_price,
        au.user_id
    FROM selected_movies sm
    JOIN movies m ON m.movie_id = sm.movie_id
    JOIN (
        VALUES
            ('The Last Horizon', TIMESTAMPTZ '2025-10-21 18:00:00+00', 1, 8.00::NUMERIC(10,2)),
            ('The Last Horizon', TIMESTAMPTZ '2025-10-22 21:00:00+00', 2, 9.50::NUMERIC(10,2)),
            ('Symphony of Shadows', TIMESTAMPTZ '2025-10-21 19:30:00+00', 3, 10.00::NUMERIC(10,2))
    ) AS s(movie_title, start_time, screen_number, base_price)
        ON sm.title = s.movie_title
    CROSS JOIN admin_user au
    ON CONFLICT DO NOTHING
    RETURNING showtime_id, screen_number, base_price
)
INSERT INTO seat_instance (
    showtime_id,
    seat_template_id,
    row_label,
    seat_number,
    type,
    price,
    created_by,
    updated_by
)
SELECT
    sh.showtime_id,
    st.seat_template_id,
    st.row_label,
    st.seat_number,
    st.type,
    (st.base_price + sh.base_price)::NUMERIC(10,2) AS price,
    au.user_id,
    au.user_id
FROM showtime_seed sh
JOIN seat_template st ON st.screen_number = sh.screen_number
CROSS JOIN admin_user au
ON CONFLICT (showtime_id, row_label, seat_number) DO NOTHING;

-- Ensure available seat counts reflect the seeded seat instances
UPDATE showtimes s
SET available_seats_count = sub.available_count
FROM (
    SELECT showtime_id, COUNT(*) AS available_count
    FROM seat_instance
    WHERE status = 'AVAILABLE'
    GROUP BY showtime_id
) AS sub
WHERE s.showtime_id = sub.showtime_id;