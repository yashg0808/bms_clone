-- ============================================
-- Drop foreign key constraints that require user authentication
-- Since auth is disabled for development, the dummy/null user IDs
-- would violate these constraints.
-- ============================================

-- Drop FK on show_seats.locked_by -> users(id)
ALTER TABLE show_seats DROP CONSTRAINT IF EXISTS show_seats_locked_by_fkey;

-- Drop FK on bookings.user_id -> users(id)
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_user_id_fkey;

-- Widen booking_number from VARCHAR(20) to VARCHAR(30) to fit generated format
-- Format: BMS-yyyyMMddHHmmss-XXXXXX = 28 chars
ALTER TABLE bookings ALTER COLUMN booking_number TYPE VARCHAR(30);

-- Widen booking_seats.seat_number from VARCHAR(10) to VARCHAR(50)
ALTER TABLE booking_seats ALTER COLUMN seat_number TYPE VARCHAR(50);
