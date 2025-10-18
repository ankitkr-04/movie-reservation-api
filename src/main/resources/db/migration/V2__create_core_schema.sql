CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'CUSTOMER'))
);

CREATE TABLE movies (
    movie_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    duration INTEGER NOT NULL,
    genre VARCHAR(50) NOT NULL,
    release_date TIMESTAMPTZ,
    poster_url VARCHAR(500),
    rating VARCHAR(10),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movies_duration CHECK (duration > 0),
    CONSTRAINT chk_movies_genre CHECK (genre IN (
        'ACTION','COMEDY','DRAMA','HORROR','THRILLER','ROMANCE','SCI_FI','ANIMATION','DOCUMENTARY','ADVENTURE'
    )),
    CONSTRAINT chk_movies_status CHECK (status IN ('ACTIVE','INACTIVE','COMING_SOON'))
);

CREATE TABLE showtimes (
    showtime_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    movie_id UUID NOT NULL REFERENCES movies(movie_id) ON DELETE RESTRICT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    screen_number SMALLINT NOT NULL,
    base_price NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    available_seats_count SMALLINT NOT NULL DEFAULT 120,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_showtimes_screen CHECK (screen_number BETWEEN 1 AND 5),
    CONSTRAINT chk_showtimes_base_price CHECK (base_price >= 0),
    CONSTRAINT chk_showtimes_status CHECK (status IN ('SCHEDULED','CANCELLED','COMPLETED'))
);

CREATE TABLE seat_template (
    seat_template_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    screen_number SMALLINT NOT NULL,
    row_label CHAR(1) NOT NULL,
    seat_number SMALLINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    base_price NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_seat_template UNIQUE (screen_number, row_label, seat_number),
    CONSTRAINT chk_seat_template_screen CHECK (screen_number BETWEEN 1 AND 5),
    CONSTRAINT chk_seat_template_row CHECK (row_label BETWEEN 'A' AND 'J'),
    CONSTRAINT chk_seat_template_number CHECK (seat_number BETWEEN 1 AND 12),
    CONSTRAINT chk_seat_template_type CHECK (type IN ('REGULAR','PREMIUM')),
    CONSTRAINT chk_seat_template_price CHECK (base_price >= 0)
);

CREATE TABLE seat_instance (
    seat_instance_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    showtime_id UUID NOT NULL REFERENCES showtimes(showtime_id) ON DELETE RESTRICT,
    seat_template_id UUID NOT NULL REFERENCES seat_template(seat_template_id) ON DELETE RESTRICT,
    row_label CHAR(1) NOT NULL,
    seat_number SMALLINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    held_at TIMESTAMPTZ,
    held_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT uq_seat_instance UNIQUE (showtime_id, row_label, seat_number),
    CONSTRAINT chk_seat_instance_row CHECK (row_label BETWEEN 'A' AND 'J'),
    CONSTRAINT chk_seat_instance_number CHECK (seat_number BETWEEN 1 AND 12),
    CONSTRAINT chk_seat_instance_type CHECK (type IN ('REGULAR','PREMIUM')),
    CONSTRAINT chk_seat_instance_price CHECK (price >= 0),
    CONSTRAINT chk_seat_instance_status CHECK (status IN ('AVAILABLE','HELD','RESERVED'))
);

CREATE TABLE reservations (
    reservation_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_reference VARCHAR(8) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    showtime_id UUID NOT NULL REFERENCES showtimes(showtime_id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_price NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT uq_reservations_booking_reference UNIQUE (booking_reference),
    CONSTRAINT chk_reservations_booking_reference CHECK (booking_reference ~ '^[A-Z0-9]{8}$'),
    CONSTRAINT chk_reservations_status CHECK (status IN ('PENDING_PAYMENT','CONFIRMED','CANCELLED','EXPIRED','REFUNDED')),
    CONSTRAINT chk_reservations_total_price CHECK (total_price >= 0)
);

CREATE TABLE reservation_seats (
    reservation_id UUID NOT NULL REFERENCES reservations(reservation_id) ON DELETE CASCADE,
    seat_instance_id UUID NOT NULL REFERENCES seat_instance(seat_instance_id) ON DELETE RESTRICT,
    price_paid NUMERIC(10,2) NOT NULL,
    PRIMARY KEY (reservation_id, seat_instance_id),
    CONSTRAINT chk_reservation_seats_price CHECK (price_paid >= 0)
);

CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reservation_id UUID NOT NULL REFERENCES reservations(reservation_id) ON DELETE RESTRICT,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'STRIPE',
    stripe_payment_intent_id VARCHAR(255) NOT NULL,
    stripe_charge_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    attempt_number SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT uq_payments_stripe_intent UNIQUE (stripe_payment_intent_id),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED','REFUNDED')),
    CONSTRAINT chk_payments_attempt CHECK (attempt_number >= 1)
);

-- Showtimes exclusion constraint to avoid overlapping schedules per screen
ALTER TABLE showtimes
    ADD CONSTRAINT showtimes_no_overlap
    EXCLUDE USING gist (
        screen_number WITH =,
        tstzrange(start_time, end_time) WITH &&
    )
    WHERE (deleted_at IS NULL);
-- Indexes for performance according to design
CREATE INDEX idx_movie_title ON movies (title);
CREATE INDEX idx_movies_genre ON movies (genre);
CREATE INDEX idx_movies_release_date ON movies (release_date);
CREATE INDEX idx_movies_active ON movies (status, deleted_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_showtimes_movie_time ON showtimes (movie_id, start_time);

CREATE INDEX idx_seat_instance_showtime_status ON seat_instance (showtime_id, status);
CREATE INDEX idx_seat_instance_seat_lookup ON seat_instance (showtime_id, row_label, seat_number);
CREATE INDEX idx_seat_instance_held ON seat_instance (status, held_at) WHERE status = 'HELD';

CREATE INDEX idx_reservations_user_created ON reservations (user_id, created_at DESC);
CREATE INDEX idx_reservations_status ON reservations (status);

CREATE INDEX idx_reservation_seats_seat ON reservation_seats (seat_instance_id);

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_reservation ON payments (reservation_id, created_at DESC);
