-- Booking service specific migration
-- Main schema is managed by the central Flyway migrations

-- Additional indexes for booking service queries
CREATE INDEX IF NOT EXISTS idx_bookings_guest_email ON bookings(guest_email);
CREATE INDEX IF NOT EXISTS idx_bookings_expires_at ON bookings(expires_at) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_show_seats_locked_expiry ON show_seats(locked_at) WHERE status = 'LOCKED';
