-- ============================================
-- Seed Data: Bookings, Booking Seats, Notifications
-- ============================================
-- Requires: shows, show_seats to be seeded first.
-- Creates realistic guest bookings in various statuses.

DO $$
DECLARE
    v_guest_names TEXT[] := ARRAY[
        'Rahul Kumar', 'Sneha Reddy', 'Amit Singh', 'Deepika Nair',
        'Vikram Desai', 'Ananya Iyer', 'Karan Mehta', 'Test User'
    ];
    v_guest_emails TEXT[] := ARRAY[
        'rahul@example.com', 'sneha@example.com', 'amit@example.com', 'deepika@example.com',
        'vikram@example.com', 'ananya@example.com', 'karan@example.com', 'test@example.com'
    ];
    v_guest_phones TEXT[] := ARRAY[
        '+919900000010', '+919900000011', '+919900000012', '+919900000013',
        '+919900000014', '+919900000015', '+919900000016', '+919900000017'
    ];

    v_show RECORD;
    v_show_seat RECORD;
    v_booking_id UUID;
    v_guest_name TEXT;
    v_guest_email TEXT;
    v_guest_phone TEXT;
    v_booking_number VARCHAR(20);
    v_total DECIMAL(10,2);
    v_convenience DECIMAL(10,2);
    v_final DECIMAL(10,2);
    v_seat_count INTEGER;
    v_booking_counter INTEGER := 0;
    v_show_count INTEGER := 0;
    v_max_bookings INTEGER := 25;
    v_idx INTEGER;
BEGIN
    -- Create bookings for past and today's shows
    FOR v_show IN
        SELECT sh.id AS show_id, sh.show_date, sh.start_time, sh.screen_id
        FROM shows sh
        WHERE sh.is_active = TRUE
          AND sh.show_date <= CURRENT_DATE + 2
        ORDER BY sh.show_date, sh.start_time
        LIMIT 60
    LOOP
        v_show_count := v_show_count + 1;
        EXIT WHEN v_booking_counter >= v_max_bookings;

        -- Pick a guest round-robin
        v_idx := (v_booking_counter % array_length(v_guest_names, 1)) + 1;
        v_guest_name := v_guest_names[v_idx];
        v_guest_email := v_guest_emails[v_idx];
        v_guest_phone := v_guest_phones[v_idx];

        -- Get 2-4 available seats for this show
        v_seat_count := 2 + (v_booking_counter % 3);  -- 2, 3, or 4 seats
        v_total := 0;

        v_booking_id := uuid_generate_v4();
        v_booking_counter := v_booking_counter + 1;
        v_booking_number := 'BMS-' || to_char(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(v_booking_counter::TEXT, 4, '0');
        v_convenience := 30.00 * v_seat_count;

        -- Calculate total from actual seat prices
        SELECT COALESCE(SUM(ss.price), 0), COUNT(*)
        INTO v_total, v_seat_count
        FROM (
            SELECT ss2.id, ss2.price
            FROM show_seats ss2
            WHERE ss2.show_id = v_show.show_id AND ss2.status = 'AVAILABLE'
            ORDER BY ss2.id
            LIMIT v_seat_count
        ) ss;

        -- Skip if not enough seats
        CONTINUE WHEN v_total = 0 OR v_seat_count = 0;

        v_final := v_total + v_convenience;

        -- Determine booking status based on counter
        IF v_booking_counter <= 15 THEN
            -- CONFIRMED bookings
            INSERT INTO bookings (id, show_id, booking_number, status, total_amount, discount_amount, final_amount,
                                  convenience_fee, discount, guest_name, guest_email, guest_phone, booked_at, version)
            VALUES (v_booking_id, v_show.show_id, v_booking_number, 'CONFIRMED',
                    v_total, 0, v_final, v_convenience, 0,
                    v_guest_name, v_guest_email, v_guest_phone,
                    NOW() - INTERVAL '1 hour' * v_booking_counter, 1);

            -- Book the seats
            FOR v_show_seat IN
                SELECT ss.id AS show_seat_id, ss.price, s.seat_number, s.row_name, s.seat_type::TEXT
                FROM show_seats ss
                JOIN seats s ON ss.seat_id = s.id
                WHERE ss.show_id = v_show.show_id AND ss.status = 'AVAILABLE'
                ORDER BY ss.id
                LIMIT v_seat_count
            LOOP
                -- Update show_seat to BOOKED
                UPDATE show_seats SET status = 'BOOKED', version = version + 1
                WHERE id = v_show_seat.show_seat_id;

                -- Create booking_seat
                INSERT INTO booking_seats (booking_id, show_seat_id, price, seat_number, seat_row, seat_type)
                VALUES (v_booking_id, v_show_seat.show_seat_id, v_show_seat.price,
                        v_show_seat.seat_number, v_show_seat.row_name, v_show_seat.seat_type);
            END LOOP;

            -- Create booking confirmation notification
            INSERT INTO notifications (booking_id, type, status, subject, content, recipient, sent_at)
            VALUES (v_booking_id, 'EMAIL', 'SENT',
                    'Booking Confirmed - ' || v_booking_number,
                    'Hi ' || v_guest_name || ', your booking ' || v_booking_number || ' has been confirmed. Total: ₹' || v_final,
                    v_guest_email,
                    NOW() - INTERVAL '1 hour' * v_booking_counter);

        ELSIF v_booking_counter <= 18 THEN
            -- CANCELLED bookings
            INSERT INTO bookings (id, show_id, booking_number, status, total_amount, discount_amount, final_amount,
                                  convenience_fee, discount, guest_name, guest_email, guest_phone, booked_at, cancelled_at, version)
            VALUES (v_booking_id, v_show.show_id, v_booking_number, 'CANCELLED',
                    v_total, 0, v_final, v_convenience, 0,
                    v_guest_name, v_guest_email, v_guest_phone,
                    NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 2);

            -- Cancellation notification
            INSERT INTO notifications (booking_id, type, status, subject, content, recipient, sent_at)
            VALUES (v_booking_id, 'EMAIL', 'SENT',
                    'Booking Cancelled - ' || v_booking_number,
                    'Hi ' || v_guest_name || ', your booking ' || v_booking_number || ' has been cancelled. Seats have been released.',
                    v_guest_email,
                    NOW() - INTERVAL '1 day');

        ELSIF v_booking_counter <= 21 THEN
            -- PENDING bookings (recently locked seats)
            INSERT INTO bookings (id, show_id, booking_number, status, total_amount, discount_amount, final_amount,
                                  convenience_fee, discount, lock_token, expires_at, version)
            VALUES (v_booking_id, v_show.show_id, v_booking_number, 'PENDING',
                    v_total, 0, v_final, v_convenience, 0,
                    'lock_' || replace(v_booking_id::TEXT, '-', ''),
                    NOW() + INTERVAL '10 minutes', 0);

            -- Lock the seats
            FOR v_show_seat IN
                SELECT ss.id AS show_seat_id
                FROM show_seats ss
                WHERE ss.show_id = v_show.show_id AND ss.status = 'AVAILABLE'
                ORDER BY ss.id
                LIMIT v_seat_count
            LOOP
                UPDATE show_seats SET status = 'LOCKED', locked_at = NOW(), version = version + 1
                WHERE id = v_show_seat.show_seat_id;
            END LOOP;

        ELSE
            -- EXPIRED bookings
            INSERT INTO bookings (id, show_id, booking_number, status, total_amount, discount_amount, final_amount,
                                  convenience_fee, discount, guest_name, guest_email, guest_phone, expires_at, version)
            VALUES (v_booking_id, v_show.show_id, v_booking_number, 'EXPIRED',
                    v_total, 0, v_final, v_convenience, 0,
                    v_guest_name, v_guest_email, v_guest_phone,
                    NOW() - INTERVAL '30 minutes', 1);
        END IF;

    END LOOP;

    RAISE NOTICE 'Bookings seeding completed! Created % bookings.', v_booking_counter;
END $$;
