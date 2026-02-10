-- Add missing columns to payments table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_id VARCHAR(200);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_amount DECIMAL(10, 2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

-- Add missing columns to booking_seats table
ALTER TABLE booking_seats ADD COLUMN IF NOT EXISTS seat_number VARCHAR(10) NOT NULL DEFAULT '';
ALTER TABLE booking_seats ADD COLUMN IF NOT EXISTS seat_row VARCHAR(10) NOT NULL DEFAULT '';
ALTER TABLE booking_seats ADD COLUMN IF NOT EXISTS seat_type VARCHAR(50) NOT NULL DEFAULT 'REGULAR';

-- Add missing columns to bookings table
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS convenience_fee DECIMAL(10, 2) DEFAULT 0;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS discount DECIMAL(10, 2) DEFAULT 0;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS coupon_id UUID;

-- Update foreign key constraint if needed (only if user_id was added)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payments' AND column_name = 'user_id') THEN
        -- Add foreign key constraint if it doesn't exist
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_user_id') THEN
            ALTER TABLE payments ADD CONSTRAINT fk_payments_user_id 
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
        END IF;
    END IF;
END $$;

-- Create index on user_id if column exists
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
