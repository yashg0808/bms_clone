-- ============================================
-- BookMyShow Clone - Indexes
-- V2__add_indexes.sql
-- ============================================

-- ============================================
-- USERS INDEXES
-- ============================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- ============================================
-- CITIES INDEXES
-- ============================================
CREATE INDEX idx_cities_name ON cities(name);
CREATE INDEX idx_cities_active ON cities(is_active) WHERE is_active = TRUE;

-- ============================================
-- THEATERS INDEXES
-- ============================================
CREATE INDEX idx_theaters_city_id ON theaters(city_id);
CREATE INDEX idx_theaters_owner_id ON theaters(owner_id) WHERE owner_id IS NOT NULL;
CREATE INDEX idx_theaters_active ON theaters(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_theaters_city_active ON theaters(city_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_theaters_name_trgm ON theaters USING gin(name gin_trgm_ops);

-- ============================================
-- SCREENS INDEXES
-- ============================================
CREATE INDEX idx_screens_theater_id ON screens(theater_id);
CREATE INDEX idx_screens_active ON screens(theater_id, is_active) WHERE is_active = TRUE;

-- ============================================
-- SEATS INDEXES
-- ============================================
CREATE INDEX idx_seats_screen_id ON seats(screen_id);
CREATE INDEX idx_seats_type ON seats(seat_type);
CREATE INDEX idx_seats_screen_row ON seats(screen_id, row_name);

-- ============================================
-- MOVIES INDEXES
-- ============================================
CREATE INDEX idx_movies_title ON movies(title);
CREATE INDEX idx_movies_language ON movies(language);
CREATE INDEX idx_movies_genre ON movies(genre);
CREATE INDEX idx_movies_release_date ON movies(release_date);
CREATE INDEX idx_movies_active ON movies(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_movies_active_release ON movies(release_date, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_movies_rating ON movies(imdb_rating DESC NULLS LAST);

-- Enable trigram extension for text search (if not already enabled)
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX idx_movies_title_trgm ON movies USING gin(title gin_trgm_ops);

-- ============================================
-- SHOWS INDEXES
-- ============================================
CREATE INDEX idx_shows_movie_id ON shows(movie_id);
CREATE INDEX idx_shows_screen_id ON shows(screen_id);
CREATE INDEX idx_shows_date ON shows(show_date);
CREATE INDEX idx_shows_movie_date ON shows(movie_id, show_date);
CREATE INDEX idx_shows_screen_date ON shows(screen_id, show_date);
CREATE INDEX idx_shows_active_date ON shows(show_date, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_shows_movie_date_active ON shows(movie_id, show_date, is_active) WHERE is_active = TRUE;

-- ============================================
-- SHOW_SEATS INDEXES (Critical for booking performance)
-- ============================================
CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);
CREATE INDEX idx_show_seats_seat_id ON show_seats(seat_id);
CREATE INDEX idx_show_seats_status ON show_seats(status);
CREATE INDEX idx_show_seats_locked_by ON show_seats(locked_by) WHERE locked_by IS NOT NULL;
CREATE INDEX idx_show_seats_locked_at ON show_seats(locked_at) WHERE locked_at IS NOT NULL;

-- Composite index for finding available seats for a show (most frequent query)
CREATE INDEX idx_show_seats_show_available ON show_seats(show_id, status) WHERE status = 'AVAILABLE';

-- Composite index for finding locked seats that may have expired
CREATE INDEX idx_show_seats_locked_expiry ON show_seats(status, locked_at) WHERE status = 'LOCKED';

-- ============================================
-- BOOKINGS INDEXES
-- ============================================
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_show_id ON bookings(show_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_booking_number ON bookings(booking_number);
CREATE INDEX idx_bookings_user_status ON bookings(user_id, status);
CREATE INDEX idx_bookings_created_at ON bookings(created_at);
CREATE INDEX idx_bookings_expires_at ON bookings(expires_at) WHERE status = 'PENDING_PAYMENT';

-- ============================================
-- BOOKING_SEATS INDEXES
-- ============================================
CREATE INDEX idx_booking_seats_booking_id ON booking_seats(booking_id);
CREATE INDEX idx_booking_seats_show_seat_id ON booking_seats(show_seat_id);

-- ============================================
-- PAYMENTS INDEXES
-- ============================================
CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_payments_gateway_order_id ON payments(gateway_order_id) WHERE gateway_order_id IS NOT NULL;
CREATE INDEX idx_payments_gateway_payment_id ON payments(gateway_payment_id) WHERE gateway_payment_id IS NOT NULL;
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- ============================================
-- NOTIFICATIONS INDEXES
-- ============================================
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_booking_id ON notifications(booking_id) WHERE booking_id IS NOT NULL;
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_pending ON notifications(status, created_at) WHERE status = 'PENDING';

-- ============================================
-- COUPONS INDEXES
-- ============================================
CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_active ON coupons(is_active, valid_from, valid_until) WHERE is_active = TRUE;

-- ============================================
-- REVIEWS INDEXES
-- ============================================
CREATE INDEX idx_reviews_movie_id ON reviews(movie_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(movie_id, rating);
CREATE INDEX idx_reviews_approved ON reviews(movie_id, is_approved) WHERE is_approved = TRUE;

-- ============================================
-- AUDIT_LOG INDEXES
-- ============================================
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_performed_by ON audit_log(performed_by) WHERE performed_by IS NOT NULL;
CREATE INDEX idx_audit_log_performed_at ON audit_log(performed_at);
