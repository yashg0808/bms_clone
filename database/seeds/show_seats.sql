-- ============================================
-- Seed Data: Show Seats
-- ============================================
-- Requires: shows, seats to be seeded first.
-- Generates a show_seat row for every (show, seat) combination
-- with pricing based on seat type and show price tiers.

DO $$
DECLARE
    v_show RECORD;
    v_seat RECORD;
    v_price DECIMAL(10,2);
    v_count INTEGER := 0;
BEGIN
    -- For each show, create show_seats from the screen's seat template
    FOR v_show IN
        SELECT sh.id AS show_id, sh.screen_id, sh.base_price, sh.premium_price, sh.recliner_price
        FROM shows sh
        WHERE sh.is_active = TRUE
          AND NOT EXISTS (SELECT 1 FROM show_seats ss WHERE ss.show_id = sh.id LIMIT 1)
    LOOP
        FOR v_seat IN
            SELECT s.id AS seat_id, s.seat_type
            FROM seats s
            WHERE s.screen_id = v_show.screen_id AND s.is_active = TRUE
        LOOP
            -- Determine price based on seat type
            CASE v_seat.seat_type
                WHEN 'RECLINER' THEN
                    v_price := COALESCE(v_show.recliner_price, v_show.base_price * 2.5);
                WHEN 'PREMIUM' THEN
                    v_price := COALESCE(v_show.premium_price, v_show.base_price * 1.5);
                WHEN 'VIP' THEN
                    v_price := COALESCE(v_show.recliner_price, v_show.base_price * 3.0);
                ELSE -- REGULAR
                    v_price := v_show.base_price;
            END CASE;

            INSERT INTO show_seats (show_id, seat_id, status, price)
            VALUES (v_show.show_id, v_seat.seat_id, 'AVAILABLE', v_price)
            ON CONFLICT (show_id, seat_id) DO NOTHING;

            v_count := v_count + 1;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Show seats seeding completed! Created % show_seat records.', v_count;
END $$;
