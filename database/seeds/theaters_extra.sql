-- ============================================
-- Seed Data: Theaters for remaining 18 cities
-- ============================================
-- Adds 1 theater with 2 screens per city so every
-- city in the DB can host shows.
-- Requires cities to be seeded first.

DO $$
DECLARE
    v_city RECORD;
    v_theater_id UUID;
    v_screen_id UUID;
    v_row CHAR;
    v_seat_type seat_type;
    v_theater_count INTEGER := 0;
    v_screen_count INTEGER := 0;
    v_seat_count INTEGER := 0;

    -- Theater names per city (realistic chain names)
    v_theater_names TEXT[][] := ARRAY[
        ARRAY['Ahmedabad',         'PVR Acropolis Mall',            'SG Highway, Ahmedabad 380054'],
        ARRAY['Jaipur',            'INOX GT Central',               'Malviya Nagar, Jaipur 302017'],
        ARRAY['Lucknow',           'PVR Phoenix Palassio',          'Shaheed Path, Lucknow 226002'],
        ARRAY['Chandigarh',        'PVR Elante Mall',               'Industrial Area Phase I, Chandigarh 160002'],
        ARRAY['Kochi',             'PVR Lulu Mall',                 'Edapally, Kochi 682024'],
        ARRAY['Indore',            'INOX Malhar Mega Mall',         'AB Road, Indore 452001'],
        ARRAY['Bhopal',            'PVR DB City Mall',              'Arera Hills, Bhopal 462011'],
        ARRAY['Nagpur',            'INOX Poonam Mall',              'Wardhaman Nagar, Nagpur 440008'],
        ARRAY['Surat',             'INOX Surat Dream',              'Vesu, Surat 395007'],
        ARRAY['Vadodara',          'PVR Inorbit Mall',              'Gorwa Road, Vadodara 390016'],
        ARRAY['Patna',             'INOX P&M Mall',                 'Patliputra, Patna 800013'],
        ARRAY['Guwahati',          'PVR Aamras Multiplex',          'GS Road, Guwahati 781005'],
        ARRAY['Visakhapatnam',     'INOX CMR Central',              'Dwaraka Nagar, Visakhapatnam 530016'],
        ARRAY['Coimbatore',        'PVR Brookefields Mall',         'Kuniyamuthur, Coimbatore 641008'],
        ARRAY['Thiruvananthapuram','PVR Lulu Mall',                 'Akkulam, Thiruvananthapuram 695017'],
        ARRAY['Mysuru',            'INOX Garuda Mall',              'Saraswathipuram, Mysuru 570009'],
        ARRAY['Dehradun',          'PVR Pacific Mall',              'Rajpur Road, Dehradun 248001'],
        ARRAY['Goa',               'INOX Panjim',                   'Patto, Panaji, Goa 403001']
    ];
    v_info TEXT[];
    v_city_id UUID;
BEGIN
    FOREACH v_info SLICE 1 IN ARRAY v_theater_names
    LOOP
        SELECT id INTO v_city_id FROM cities WHERE name = v_info[1] LIMIT 1;
        IF v_city_id IS NULL THEN
            RAISE NOTICE 'City % not found, skipping', v_info[1];
            CONTINUE;
        END IF;

        -- Skip if this city already has theaters
        IF EXISTS (SELECT 1 FROM theaters WHERE city_id = v_city_id) THEN
            RAISE NOTICE 'City % already has theaters, skipping', v_info[1];
            CONTINUE;
        END IF;

        v_theater_id := uuid_generate_v4();
        INSERT INTO theaters (id, name, city_id, address, total_screens)
        VALUES (v_theater_id, v_info[2], v_city_id, v_info[3], 2);
        v_theater_count := v_theater_count + 1;

        -- Screen 1: Standard (120 seats: 10 rows x 12 cols)
        v_screen_id := uuid_generate_v4();
        INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
        VALUES (v_screen_id, v_theater_id, 'Screen 1', 120, 'STANDARD');
        v_screen_count := v_screen_count + 1;

        FOR r IN 1..10 LOOP
            v_row := CHR(64 + r);
            FOR c IN 1..12 LOOP
                IF r <= 2 THEN v_seat_type := 'RECLINER';
                ELSIF r <= 5 THEN v_seat_type := 'PREMIUM';
                ELSE v_seat_type := 'REGULAR';
                END IF;
                INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
                VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
                v_seat_count := v_seat_count + 1;
            END LOOP;
        END LOOP;

        -- Screen 2: IMAX (150 seats: 10 rows x 15 cols)
        v_screen_id := uuid_generate_v4();
        INSERT INTO screens (id, theater_id, name, total_seats, screen_type)
        VALUES (v_screen_id, v_theater_id, 'Screen 2 - IMAX', 150, 'IMAX');
        v_screen_count := v_screen_count + 1;

        FOR r IN 1..10 LOOP
            v_row := CHR(64 + r);
            FOR c IN 1..15 LOOP
                IF r <= 2 THEN v_seat_type := 'RECLINER';
                ELSIF r <= 5 THEN v_seat_type := 'PREMIUM';
                ELSE v_seat_type := 'REGULAR';
                END IF;
                INSERT INTO seats (screen_id, seat_number, row_name, column_number, seat_type)
                VALUES (v_screen_id, v_row || c, v_row, c, v_seat_type);
                v_seat_count := v_seat_count + 1;
            END LOOP;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Extra theaters seeding completed! % theaters, % screens, % seats created.',
        v_theater_count, v_screen_count, v_seat_count;
END $$;
