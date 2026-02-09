-- ============================================
-- BookMyShow Clone - Initial Schema
-- V1__initial_schema.sql
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- ENUM TYPES
-- ============================================
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ADMIN', 'THEATER_OWNER');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
CREATE TYPE seat_type AS ENUM ('REGULAR', 'PREMIUM', 'RECLINER', 'VIP');
CREATE TYPE seat_status AS ENUM ('AVAILABLE', 'LOCKED', 'BOOKED');
CREATE TYPE booking_status AS ENUM ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED');
CREATE TYPE payment_status AS ENUM ('INITIATED', 'PROCESSING', 'SUCCESS', 'FAILED', 'REFUNDED');
CREATE TYPE payment_method AS ENUM ('CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'NET_BANKING', 'WALLET');
CREATE TYPE notification_type AS ENUM ('EMAIL', 'SMS', 'PUSH');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED');

-- ============================================
-- USERS TABLE
-- ============================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100),
    phone           VARCHAR(20),
    role            user_role NOT NULL DEFAULT 'CUSTOMER',
    status          user_status NOT NULL DEFAULT 'ACTIVE',
    avatar_url      VARCHAR(500),
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE users IS 'Stores user account information for authentication and profile';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password';

-- ============================================
-- CITIES TABLE
-- ============================================
CREATE TABLE cities (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL,
    state       VARCHAR(100) NOT NULL,
    country     VARCHAR(100) NOT NULL DEFAULT 'India',
    latitude    DECIMAL(10, 7),
    longitude   DECIMAL(10, 7),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE cities IS 'Cities where BookMyShow operates';

-- ============================================
-- THEATERS TABLE
-- ============================================
CREATE TABLE theaters (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(200) NOT NULL,
    city_id     UUID NOT NULL REFERENCES cities(id) ON DELETE RESTRICT,
    address     TEXT NOT NULL,
    latitude    DECIMAL(10, 7),
    longitude   DECIMAL(10, 7),
    phone       VARCHAR(20),
    email       VARCHAR(255),
    owner_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    total_screens INTEGER NOT NULL DEFAULT 1,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE theaters IS 'Movie theaters/cinemas in various cities';

-- ============================================
-- SCREENS TABLE
-- ============================================
CREATE TABLE screens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    theater_id      UUID NOT NULL REFERENCES theaters(id) ON DELETE CASCADE,
    name            VARCHAR(50) NOT NULL,
    total_seats     INTEGER NOT NULL,
    screen_type     VARCHAR(50) DEFAULT 'STANDARD',  -- STANDARD, IMAX, 4DX, DOLBY
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (theater_id, name)
);

COMMENT ON TABLE screens IS 'Individual screens within a theater';

-- ============================================
-- SEATS TABLE (Template seats for a screen)
-- ============================================
CREATE TABLE seats (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    screen_id       UUID NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    seat_number     VARCHAR(10) NOT NULL,
    row_name        VARCHAR(5) NOT NULL,
    column_number   INTEGER NOT NULL,
    seat_type       seat_type NOT NULL DEFAULT 'REGULAR',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (screen_id, row_name, column_number)
);

COMMENT ON TABLE seats IS 'Template seat layout for each screen. Used to generate show_seats for each show.';

-- ============================================
-- MOVIES TABLE
-- ============================================
CREATE TABLE movies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    duration_minutes INTEGER NOT NULL,
    language        VARCHAR(50) NOT NULL,
    genre           VARCHAR(200) NOT NULL,
    release_date    DATE NOT NULL,
    end_date        DATE,
    rating          VARCHAR(10),           -- U, UA, A, S
    imdb_rating     DECIMAL(3, 1),
    poster_url      VARCHAR(500),
    banner_url      VARCHAR(500),
    trailer_url     VARCHAR(500),
    cast_info       JSONB,                 -- Array of {name, role, image}
    crew_info       JSONB,                 -- Array of {name, role, image}
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE movies IS 'Movie catalog with metadata';
COMMENT ON COLUMN movies.cast_info IS 'JSON array: [{name, role, imageUrl}]';

-- ============================================
-- SHOWS TABLE
-- ============================================
CREATE TABLE shows (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    movie_id        UUID NOT NULL REFERENCES movies(id) ON DELETE RESTRICT,
    screen_id       UUID NOT NULL REFERENCES screens(id) ON DELETE RESTRICT,
    show_date       DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    base_price      DECIMAL(10, 2) NOT NULL,
    premium_price   DECIMAL(10, 2),
    recliner_price  DECIMAL(10, 2),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (screen_id, show_date, start_time)
);

COMMENT ON TABLE shows IS 'Movie show schedules. Each show is tied to a screen and date/time.';

-- ============================================
-- SHOW_SEATS TABLE (Per-show seat instances)
-- ============================================
CREATE TABLE show_seats (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    show_id         UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_id         UUID NOT NULL REFERENCES seats(id) ON DELETE RESTRICT,
    status          seat_status NOT NULL DEFAULT 'AVAILABLE',
    price           DECIMAL(10, 2) NOT NULL,
    locked_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    locked_at       TIMESTAMP WITH TIME ZONE,
    version         INTEGER NOT NULL DEFAULT 0,   -- Optimistic locking
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (show_id, seat_id)
);

COMMENT ON TABLE show_seats IS 'Individual seat instances per show with availability status. Uses optimistic locking via version field.';
COMMENT ON COLUMN show_seats.version IS 'Used for optimistic locking to prevent double-booking';
COMMENT ON COLUMN show_seats.locked_by IS 'User who currently holds the temporary lock on this seat';

-- ============================================
-- BOOKINGS TABLE
-- ============================================
CREATE TABLE bookings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    show_id         UUID NOT NULL REFERENCES shows(id) ON DELETE RESTRICT,
    booking_number  VARCHAR(20) NOT NULL UNIQUE,
    status          booking_status NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount    DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) DEFAULT 0,
    final_amount    DECIMAL(10, 2) NOT NULL,
    lock_token      VARCHAR(100),
    booked_at       TIMESTAMP WITH TIME ZONE,
    expires_at      TIMESTAMP WITH TIME ZONE,
    cancelled_at    TIMESTAMP WITH TIME ZONE,
    version         INTEGER NOT NULL DEFAULT 0,   -- Optimistic locking
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE bookings IS 'Booking records for movie tickets. Uses optimistic locking.';
COMMENT ON COLUMN bookings.booking_number IS 'Human-readable booking reference (e.g., BMS-20260101-XXXX)';

-- ============================================
-- BOOKING_SEATS TABLE (Junction table)
-- ============================================
CREATE TABLE booking_seats (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id      UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    show_seat_id    UUID NOT NULL REFERENCES show_seats(id) ON DELETE RESTRICT,
    price           DECIMAL(10, 2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (booking_id, show_seat_id)
);

COMMENT ON TABLE booking_seats IS 'Maps which seats belong to which booking';

-- ============================================
-- PAYMENTS TABLE
-- ============================================
CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id          UUID NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    amount              DECIMAL(10, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_method      payment_method,
    status              payment_status NOT NULL DEFAULT 'INITIATED',
    gateway_order_id    VARCHAR(200),
    gateway_payment_id  VARCHAR(200),
    gateway_signature   VARCHAR(500),
    idempotency_key     VARCHAR(100) UNIQUE,
    failure_reason      TEXT,
    paid_at             TIMESTAMP WITH TIME ZONE,
    refunded_at         TIMESTAMP WITH TIME ZONE,
    metadata            JSONB,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE payments IS 'Payment records with gateway integration details';
COMMENT ON COLUMN payments.idempotency_key IS 'Ensures exactly-once payment processing';

-- ============================================
-- NOTIFICATIONS TABLE
-- ============================================
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    booking_id      UUID REFERENCES bookings(id) ON DELETE SET NULL,
    type            notification_type NOT NULL,
    status          notification_status NOT NULL DEFAULT 'PENDING',
    subject         VARCHAR(300),
    content         TEXT NOT NULL,
    recipient       VARCHAR(255) NOT NULL,      -- email or phone
    sent_at         TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE notifications IS 'Notification log for emails, SMS, and push notifications';

-- ============================================
-- COUPONS TABLE
-- ============================================
CREATE TABLE coupons (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(50) NOT NULL UNIQUE,
    description     TEXT,
    discount_type   VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',  -- PERCENTAGE, FIXED
    discount_value  DECIMAL(10, 2) NOT NULL,
    max_discount    DECIMAL(10, 2),
    min_order_value DECIMAL(10, 2) DEFAULT 0,
    valid_from      TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until     TIMESTAMP WITH TIME ZONE NOT NULL,
    max_uses        INTEGER,
    current_uses    INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE coupons IS 'Discount coupons for bookings';

-- ============================================
-- USER_COUPONS TABLE (track coupon usage)
-- ============================================
CREATE TABLE user_coupons (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    coupon_id       UUID NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    booking_id      UUID REFERENCES bookings(id) ON DELETE SET NULL,
    used_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, coupon_id, booking_id)
);

-- ============================================
-- REVIEWS TABLE
-- ============================================
CREATE TABLE reviews (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    movie_id        UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    rating          INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 10),
    review_text     TEXT,
    is_approved     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, movie_id)
);

COMMENT ON TABLE reviews IS 'User reviews and ratings for movies';

-- ============================================
-- AUDIT_LOG TABLE
-- ============================================
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,
    old_values      JSONB,
    new_values      JSONB,
    performed_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    performed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_address      VARCHAR(45)
);

COMMENT ON TABLE audit_log IS 'Audit trail for critical operations';

-- ============================================
-- UPDATED_AT TRIGGER FUNCTION
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at triggers
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_cities_updated_at BEFORE UPDATE ON cities FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_theaters_updated_at BEFORE UPDATE ON theaters FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_screens_updated_at BEFORE UPDATE ON screens FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_movies_updated_at BEFORE UPDATE ON movies FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_shows_updated_at BEFORE UPDATE ON shows FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_show_seats_updated_at BEFORE UPDATE ON show_seats FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_bookings_updated_at BEFORE UPDATE ON bookings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON reviews FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
