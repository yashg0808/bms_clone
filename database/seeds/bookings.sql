-- ============================================
-- Seed Data: Bookings, Booking Seats, Payments,
--            Notifications, User Coupons
-- ============================================
-- Requires: users, shows, show_seats, coupons to be seeded first.
-- Creates realistic bookings in various statuses.

DO $$
DECLARE
    v_user_ids UUID[] := ARRAY[
        'a0000000-0000-0000-0000-000000000010',
        'a0000000-0000-0000-0000-000000000011',
        'a0000000-0000-0000-0000-000000000012',
        'a0000000-0000-0000-0000-000000000013',
        'a0000000-0000-0000-0000-000000000014',
        'a0000000-0000-0000-0000-000000000015',
        'a0000000-0000-0000-0000-000000000016',
        'a0000000-0000-0000-0000-000000000017'
    ]::UUID[];

    v_show RECORD;
    v_show_seat RECORD;
    v_booking_id UUID;
    v_payment_id UUID;
    v_user_id UUID;
    v_booking_number VARCHAR(20);
    v_total DECIMAL(10,2);
    v_convenience DECIMAL(10,2);
    v_final DECIMAL(10,2);
    v_seat_count INTEGER;
    v_booking_counter INTEGER := 0;
    v_coupon_id UUID;
    v_discount DECIMAL(10,2);
    v_show_count INTEGER := 0;
    v_max_bookings INTEGER := 25;
BEGIN
    -- Create confirmed bookings for past and today's shows
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

        -- Pick a user round-robin
        v_user_id := v_user_ids[((v_booking_counter) % array_length(v_user_ids, 1)) + 1];

        -- Get 2-4 available seats for this show
        v_seat_count := 2 + (v_booking_counter % 3);  -- 2, 3, or 4 seats
        v_total := 0;

        v_booking_id := uuid_generate_v4();
        v_booking_counter := v_booking_counter + 1;
        v_booking_number := 'BMS-' || to_char(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(v_booking_counter::TEXT, 4, '0');
        v_convenience := 30.00 * v_seat_count;
        v_discount := 0;

        -- Apply coupon to some bookings
        IF v_booking_counter % 4 = 0 THEN
            SELECT id, discount_value INTO v_coupon_id, v_discount
            FROM coupons WHERE code = 'FLAT100' AND is_active = TRUE LIMIT 1;
        ELSE
            v_coupon_id := NULL;
        END IF;

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

        v_final := v_total + v_convenience - v_discount;
        IF v_final < 0 THEN v_final := 0; END IF;

        -- Determine booking status based on counter
        IF v_booking_counter <= 15 THEN
            -- CONFIRMED bookings
            INSERT INTO bookings (id, user_id, show_id, booking_number, status, total_amount, discount_amount, final_amount,
                                  convenience_fee, discount, coupon_id, booked_at, version)
            VALUES (v_booking_id, v_user_id, v_show.show_id, v_booking_number, 'CONFIRMED',
                    v_total, v_discount, v_final, v_convenience, v_discount, v_coupon_id,
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
                UPDATE show_seats SET status = 'BOOKED', locked_by = v_user_id, version = version + 1
                WHERE id = v_show_seat.show_seat_id;

                -- Create booking_seat
                INSERT INTO booking_seats (booking_id, show_seat_id, price, seat_number, seat_row, seat_type)
                VALUES (v_booking_id, v_show_seat.show_seat_id, v_show_seat.price,
                        v_show_seat.seat_number, v_show_seat.row_name, v_show_seat.seat_type);
            END LOOP;

            -- Create successful payment
            v_payment_id := uuid_generate_v4();
            INSERT INTO payments (id, booking_id, user_id, amount, currency, payment_method, status,
                                  gateway_order_id, gateway_payment_id, idempotency_key, paid_at, version)
            VALUES (v_payment_id, v_booking_id, v_user_id, v_final, 'INR',
                    (ARRAY['UPI', 'CREDIT_CARD', 'DEBIT_CARD', 'NET_BANKING', 'WALLET']::payment_method[])[1 + (v_booking_counter % 5)],
                    'SUCCESS',
                    'order_' || replace(v_booking_id::TEXT, '-', ''),
                    'pay_' || replace(v_payment_id::TEXT, '-', ''),
                    'idem_' || replace(v_booking_id::TEXT, '-', ''),
                    NOW() - INTERVAL '1 hour' * v_booking_counter,
                    1);

            -- Create booking confirmation notification
            INSERT INTO notifications (user_id, booking_id, type, status, subject, content, recipient, sent_at)
            VALUES (v_user_id, v_booking_id, 'EMAIL', 'SENT',
                    'Booking Confirmed - ' || v_booking_number,
                    'Your booking ' || v_booking_number || ' has been confirmed. Total: ₹' || v_final,
                    (SELECT email FROM users WHERE id = v_user_id),
                    NOW() - INTERVAL '1 hour' * v_booking_counter);

            INSERT INTO notifications (user_id, booking_id, type, status, subject, content, recipient, sent_at)
            VALUES (v_user_id, v_booking_id, 'SMS', 'SENT',
                    'Booking Confirmed',
                    'BMS: Booking ' || v_booking_number || ' confirmed. Amount: ₹' || v_final || '. Enjoy your movie!',
                    (SELECT phone FROM users WHERE id = v_user_id),
                    NOW() - INTERVAL '1 hour' * v_booking_counter);

            -- Track coupon usage
            IF v_coupon_id IS NOT NULL THEN
                INSERT INTO user_coupons (user_id, coupon_id, booking_id) VALUES (v_user_id, v_coupon_id, v_booking_id);
                UPDATE coupons SET current_uses = current_uses + 1 WHERE id = v_coupon_id;
            END IF;

        ELSIF v_booking_counter <= 18 THEN
            -- CANCELLED bookings
            INSERT INTO bookings (id, user_id, show_id, booking_number, status, total_amount, discount_amount, final_amount,
                                  convenience_fee, discount, booked_at, cancelled_at, version)
            VALUES (v_booking_id, v_user_id, v_show.show_id, v_booking_number, 'CANCELLED',
                    v_total, 0, v_total + v_convenience, v_convenience, 0,
                    NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 2);

            -- Create refunded payment
            v_payment_id := uuid_generate_v4();
            INSERT INTO payments (id, booking_id, user_id, amount, currency, payment_method, status,
                                  gateway_order_id, gateway_payment_id, idempotency_key,
                                  paid_at, refunded_at, refund_amount, version)
            VALUES (v_payment_id, v_booking_id, v_user_id, v_total + v_convenience, 'INR', 'UPI', 'REFUNDED',
                    'order_' || replace(v_booking_id::TEXT, '-', ''),
                    'pay_' || replace(v_payment_id::TEXT, '-', ''),
                    'idem_' || replace(v_booking_id::TEXT, '-', ''),
                    NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', v_total + v_convenience, 2);

            -- Cancellation notification
            INSERT INTO notifications (user_id, booking_id, type, status, subject, content, recipient, sent_at)
            VALUES (v_user_id, v_booking_id, 'EMAIL', 'SENT',
                    'Booking Cancelled - ' || v_booking_number,
                    'Your booking ' || v_booking_number || ' has been cancelled. Refund of ₹' || (v_total + v_convenience) || ' initiated.',
                    (SELECT email FROM users WHERE id = v_user_id),
                    NOW() - INTERVAL '1 day');

        ELSIF v_booking_counter <= 21 THEN
            -- PENDING_PAYMENT bookings (recent)
            INSERT INTO bookings (id, user_id, show_id, booking_number, status, total_amount, discount_amount, final_amount,
                                  convenience_fee, discount, lock_token, expires_at, version)
            VALUES (v_booking_id, v_user_id, v_show.show_id, v_booking_number, 'PENDING_PAYMENT',
                    v_total, 0, v_total + v_convenience, v_convenience, 0,
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
                UPDATE show_seats SET status = 'LOCKED', locked_by = v_user_id, locked_at = NOW(), version = version + 1
                WHERE id = v_show_seat.show_seat_id;
            END LOOP;

            -- Create initiated payment
            v_payment_id := uuid_generate_v4();
            INSERT INTO payments (id, booking_id, user_id, amount, currency, status,
                                  gateway_order_id, idempotency_key, version)
            VALUES (v_payment_id, v_booking_id, v_user_id, v_total + v_convenience, 'INR', 'INITIATED',
                    'order_' || replace(v_booking_id::TEXT, '-', ''),
                    'idem_' || replace(v_booking_id::TEXT, '-', ''),
                    0);

        ELSE
            -- EXPIRED bookings
            INSERT INTO bookings (id, user_id, show_id, booking_number, status, total_amount, discount_amount, final_amount,
                                  convenience_fee, discount, expires_at, version)
            VALUES (v_booking_id, v_user_id, v_show.show_id, v_booking_number, 'EXPIRED',
                    v_total, 0, v_total + v_convenience, v_convenience, 0,
                    NOW() - INTERVAL '30 minutes', 1);

            -- Create failed payment
            v_payment_id := uuid_generate_v4();
            INSERT INTO payments (id, booking_id, user_id, amount, currency, status,
                                  gateway_order_id, idempotency_key, failure_reason, version)
            VALUES (v_payment_id, v_booking_id, v_user_id, v_total + v_convenience, 'INR', 'FAILED',
                    'order_' || replace(v_booking_id::TEXT, '-', ''),
                    'idem_' || replace(v_booking_id::TEXT, '-', ''),
                    'Payment timeout - session expired', 1);
        END IF;

    END LOOP;

    RAISE NOTICE 'Bookings seeding completed! Created % bookings.', v_booking_counter;
END $$;
