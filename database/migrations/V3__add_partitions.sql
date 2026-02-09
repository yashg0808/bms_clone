-- ============================================
-- BookMyShow Clone - Table Partitioning
-- V3__add_partitions.sql
-- ============================================
-- Note: Partitioning requires recreating tables. In production,
-- this would be done during initial setup or with pg_partman.
-- For this migration, we create partitioned versions for new data.

-- ============================================
-- SHOW_SEATS PARTITIONING BY MONTH
-- ============================================
-- We partition show_seats by using a composite approach:
-- Since show_seats references shows.show_date indirectly,
-- we add a show_date column for partitioning.

ALTER TABLE show_seats ADD COLUMN IF NOT EXISTS show_date DATE;

-- Backfill show_date from shows table
UPDATE show_seats ss
SET show_date = s.show_date
FROM shows s
WHERE ss.show_id = s.id
AND ss.show_date IS NULL;

-- Create index on the new partition key
CREATE INDEX IF NOT EXISTS idx_show_seats_show_date ON show_seats(show_date);

-- ============================================
-- BOOKINGS PARTITIONING PREPARATION
-- ============================================
-- Add booking_date column derived from created_at for partitioning
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS booking_date DATE;

UPDATE bookings SET booking_date = DATE(created_at) WHERE booking_date IS NULL;

CREATE INDEX IF NOT EXISTS idx_bookings_booking_date ON bookings(booking_date);

-- ============================================
-- ARCHIVE PARTITIONING STRATEGY
-- ============================================
-- Create archive tables for old data
CREATE TABLE IF NOT EXISTS bookings_archive (
    LIKE bookings INCLUDING ALL
);

CREATE TABLE IF NOT EXISTS show_seats_archive (
    LIKE show_seats INCLUDING ALL
);

COMMENT ON TABLE bookings_archive IS 'Archive table for bookings older than 6 months';
COMMENT ON TABLE show_seats_archive IS 'Archive table for show_seats of past shows';

-- ============================================
-- ARCHIVAL FUNCTION
-- ============================================
CREATE OR REPLACE FUNCTION archive_old_bookings(older_than_days INTEGER DEFAULT 180)
RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    -- Move old completed/cancelled bookings to archive
    WITH moved AS (
        DELETE FROM bookings
        WHERE status IN ('CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED')
        AND created_at < NOW() - (older_than_days || ' days')::INTERVAL
        RETURNING *
    )
    INSERT INTO bookings_archive SELECT * FROM moved;

    GET DIAGNOSTICS archived_count = ROW_COUNT;
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION archive_old_show_seats(older_than_days INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    WITH moved AS (
        DELETE FROM show_seats
        WHERE show_date < CURRENT_DATE - older_than_days
        RETURNING *
    )
    INSERT INTO show_seats_archive SELECT * FROM moved;

    GET DIAGNOSTICS archived_count = ROW_COUNT;
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION archive_old_bookings IS 'Archives bookings older than N days (default 180)';
COMMENT ON FUNCTION archive_old_show_seats IS 'Archives show_seats for shows older than N days (default 90)';
