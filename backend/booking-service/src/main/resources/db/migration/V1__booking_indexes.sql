-- Booking service specific migration
-- Main schema is managed by the central Flyway migrations

-- Additional indexes for booking service queries
CREATE INDEX IF NOT EXISTS idx_bookings_user_id_status ON bookings(user_id, status);
CREATE INDEX IF NOT EXISTS idx_bookings_expires_at ON bookings(expires_at) WHERE status = 'PENDING_PAYMENT';
CREATE INDEX IF NOT EXISTS idx_show_seats_locked_expiry ON show_seats(locked_at) WHERE status = 'LOCKED';
