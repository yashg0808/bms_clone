-- ============================================
-- V6: Remove user/payment concepts, add guest booking
-- ============================================
-- This migration removes the user authentication and payment
-- processing concepts. Bookings now capture guest details
-- (name, email, phone) directly. No login required.

-- ============================================
-- 1. Add guest columns to bookings table
-- ============================================
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS guest_name VARCHAR(200);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS guest_email VARCHAR(255);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS guest_phone VARCHAR(20);

-- Make user_id nullable (no longer required)
ALTER TABLE bookings ALTER COLUMN user_id DROP NOT NULL;

-- ============================================
-- 2. Remove user FK from show_seats.locked_by
--    (already dropped in V5, but ensure it's gone)
-- ============================================
ALTER TABLE show_seats DROP CONSTRAINT IF EXISTS show_seats_locked_by_fkey;

-- ============================================
-- 3. Drop payment-related tables
-- ============================================
DROP TABLE IF EXISTS user_coupons CASCADE;
DROP TABLE IF EXISTS payments CASCADE;

-- ============================================
-- 4. Drop user-related tables
-- ============================================
-- Remove FK constraints referencing users first
ALTER TABLE theaters DROP CONSTRAINT IF EXISTS theaters_owner_id_fkey;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_user_id_fkey;
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_user_id_fkey;
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_performed_by_fkey;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_user_id_fkey;

-- Now drop the users table
DROP TABLE IF EXISTS users CASCADE;

-- ============================================
-- 5. Make notifications.user_id nullable
--    (notifications now use guest email/phone)
-- ============================================
ALTER TABLE notifications ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS guest_email VARCHAR(255);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS guest_phone VARCHAR(20);

-- ============================================
-- 6. Drop user-related enums (if not used elsewhere)
-- ============================================
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS user_status CASCADE;
DROP TYPE IF EXISTS payment_status CASCADE;
DROP TYPE IF EXISTS payment_method CASCADE;

-- ============================================
-- 7. Remove user-related indexes
-- ============================================
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_users_phone;
DROP INDEX IF EXISTS idx_users_role;
DROP INDEX IF EXISTS idx_users_status;
DROP INDEX IF EXISTS idx_users_created_at;
DROP INDEX IF EXISTS idx_payments_booking_id;
DROP INDEX IF EXISTS idx_payments_status;
DROP INDEX IF EXISTS idx_payments_idempotency_key;
DROP INDEX IF EXISTS idx_payments_gateway_order_id;
DROP INDEX IF EXISTS idx_payments_gateway_payment_id;
DROP INDEX IF EXISTS idx_payments_created_at;
DROP INDEX IF EXISTS idx_payments_user_id;

-- ============================================
-- 8. Update booking status enum - remove REFUNDED, keep others
-- ============================================
-- PENDING_PAYMENT becomes PENDING (seats locked, waiting for guest details)
-- No change needed to the enum values since they're just strings in the app

-- ============================================
-- 9. Add indexes for guest lookups
-- ============================================
CREATE INDEX IF NOT EXISTS idx_bookings_guest_email ON bookings(guest_email) WHERE guest_email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bookings_guest_phone ON bookings(guest_phone) WHERE guest_phone IS NOT NULL;
