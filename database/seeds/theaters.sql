-- ============================================
-- Seed Data: Theaters & Screens & Seats
-- ============================================
-- This script requires cities to be seeded first.
-- It uses a DO block with variables for city IDs.

DO $$
DECLARE
    v_mumbai_id UUID;
    v_delhi_id UUID;
    v_bengaluru_id UUID;
    v_hyderabad_id UUID;
    v_chennai_id UUID;
    v_pune_id UUID;
    v_kolkata_id UUID;
    
    v_theater_id UUID;
    v_screen_id UUID;
    v_row CHAR;
    v_col INTEGER;
    v_seat_type seat_type;
BEGIN
    -- Get city IDs
    SELECT id INTO v_mumbai_id FROM cities WHERE name = 'Mumbai' LIMIT 1;
    SELECT id INTO v_delhi_id FROM cities WHERE name = 'Delhi' LIMIT 1;
    SELECT id INTO v_bengaluru_id FROM cities WHERE name = 'Bengaluru' LIMIT 1;
    SELECT id INTO v_hyderabad_id FROM cities WHERE name = 'Hyderabad' LIMIT 1;
    SELECT id INTO v_chennai_id FROM cities WHERE name = 'Chennai' LIMIT 1;
    SELECT id INTO v_pune_id FROM cities WHERE name = 'Pune' LIMIT 1;
    SELECT id INTO v_kolkata_id FROM cities WHERE name = 'Kolkata' LIMIT 1;

    -- ==============================
    -- MUMBAI THEATERS
    -- ==============================
    
    -- Theater 1: PVR Phoenix
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'PVR Phoenix MarketCity', v_mumbai_id, 'LBS Marg, Kurla West, Mumbai 400070', 6);
    
    -- Screen 1 (Standard, 120 seats: 10 rows x 12 cols)
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 1', 120, 'STANDARD');
    
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);  -- A=65
        FOR c IN 1..12 LOOP
            IF r <= 2 THEN v_seat_type := 'RECLINER';
            ELSIF r <= 5 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;
    
    -- Screen 2 (IMAX, 200 seats: 10 rows x 20 cols)
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 2 - IMAX', 200, 'IMAX');
    
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..20 LOOP
            IF r <= 2 THEN v_seat_type := 'RECLINER';
            ELSIF r <= 5 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- Screen 3 (4DX, 80 seats: 8 rows x 10 cols)
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 3 - 4DX', 80, '4DX');
    
    FOR r IN 1..8 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..10 LOOP
            IF r <= 2 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- Theater 2: INOX R-City
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'INOX R-City Mall', v_mumbai_id, 'LBS Road, Ghatkopar West, Mumbai 400086', 5);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Insignia', 60, 'DOLBY');
    FOR r IN 1..6 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..10 LOOP
            IF r <= 2 THEN v_seat_type := 'RECLINER';
            ELSE v_seat_type := 'PREMIUM';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 2', 150, 'STANDARD');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..15 LOOP
            IF r <= 3 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- Theater 3: Cinepolis
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'Cinepolis Andheri', v_mumbai_id, 'Fun Republic, Andheri West, Mumbai 400053', 4);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 1', 100, 'STANDARD');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..10 LOOP
            IF r <= 2 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- ==============================
    -- DELHI THEATERS
    -- ==============================
    
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'PVR Select Citywalk', v_delhi_id, 'A-3, District Centre, Saket, New Delhi 110017', 7);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Director''s Cut', 40, 'DOLBY');
    FOR r IN 1..4 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..10 LOOP
            v_seat_type := 'RECLINER';
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'IMAX', 250, 'IMAX');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..25 LOOP
            IF r <= 2 THEN v_seat_type := 'RECLINER';
            ELSIF r <= 5 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'INOX Nehru Place', v_delhi_id, 'Epicuria Mall, Nehru Place, New Delhi 110019', 4);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 1', 120, 'STANDARD');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..12 LOOP
            IF r <= 3 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- ==============================
    -- BENGALURU THEATERS
    -- ==============================
    
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'PVR Orion Mall', v_bengaluru_id, 'Brigade Gateway, Rajajinagar, Bengaluru 560055', 8);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Gold Class', 50, 'DOLBY');
    FOR r IN 1..5 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..10 LOOP
            IF r <= 2 THEN v_seat_type := 'RECLINER';
            ELSE v_seat_type := 'PREMIUM';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 2', 180, 'STANDARD');
    FOR r IN 1..12 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..15 LOOP
            IF r <= 3 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'INOX Garuda Mall', v_bengaluru_id, 'Magrath Road, Ashok Nagar, Bengaluru 560025', 5);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 1', 100, 'STANDARD');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..10 LOOP
            IF r <= 2 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- ==============================
    -- HYDERABAD THEATERS
    -- ==============================
    
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'AMB Cinemas', v_hyderabad_id, 'Gachibowli, Hyderabad 500032', 4);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 1 - IMAX', 300, 'IMAX');
    FOR r IN 1..12 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..25 LOOP
            IF r <= 3 THEN v_seat_type := 'RECLINER';
            ELSIF r <= 6 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'PVR Inorbit Mall', v_hyderabad_id, 'Inorbit Mall, Madhapur, Hyderabad 500081', 6);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 1', 100, 'STANDARD');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..10 LOOP
            IF r <= 2 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- ==============================
    -- CHENNAI THEATERS
    -- ==============================
    
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'SPI Palazzo', v_chennai_id, 'Forum Vijaya Mall, Vadapalani, Chennai 600026', 5);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'The Luxury Screen', 45, 'DOLBY');
    FOR r IN 1..5 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..9 LOOP
            v_seat_type := 'RECLINER';
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 2', 140, 'STANDARD');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..14 LOOP
            IF r <= 3 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- ==============================
    -- PUNE THEATERS
    -- ==============================
    
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'PVR Seasons Mall', v_pune_id, 'Magarpatta City, Hadapsar, Pune 411028', 5);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 1', 130, 'STANDARD');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..13 LOOP
            IF r <= 3 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    -- ==============================
    -- KOLKATA THEATERS
    -- ==============================
    
    v_theater_id := uuid_generate_v4();
    INSERT INTO theaters (id, name, city_id, address, total_screens)
    VALUES (v_theater_id, 'INOX South City Mall', v_kolkata_id, 'Prince Anwar Shah Road, Kolkata 700068', 6);
    
    v_screen_id := uuid_generate_v4();
    INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
    VALUES (v_screen_id, v_theater_id, 'Screen 1', 110, 'STANDARD');
    FOR r IN 1..10 LOOP
        v_row := CHR(64 + r);
        FOR c IN 1..11 LOOP
            IF r <= 3 THEN v_seat_type := 'PREMIUM';
            ELSE v_seat_type := 'REGULAR';
            END IF;
            INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
            VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Theater seeding completed successfully!';
END $$;
