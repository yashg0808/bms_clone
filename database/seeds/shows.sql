-- ============================================
-- Seed Data: Shows (Movie Showtimes)
-- ============================================
-- Ensures EVERY movie has at least 1 show in EVERY city
-- every day for 7 days. Dynamically generates enough
-- unique timeslots per screen to avoid conflicts.

DO $$
DECLARE
    v_city RECORD;
    v_screens UUID[];
    v_screen_types TEXT[];
    v_screen_count INTEGER;
    v_movies UUID[];
    v_movie_count INTEGER;
    v_day INTEGER;
    v_show_date DATE;
    v_mi INTEGER;
    v_si INTEGER;           -- screen index (1-based into array)
    v_time_offset INTEGER;  -- minutes offset for the slot on that screen
    v_start TIME;
    v_end TIME;
    v_slots_per_screen INTEGER;
    v_base DECIMAL(10,2);
    v_prem DECIMAL(10,2);
    v_recl DECIMAL(10,2);
    v_show_count INTEGER := 0;
    v_inserted INTEGER := 0;
BEGIN
    SELECT array_agg(id ORDER BY id) INTO v_movies FROM movies WHERE is_active = TRUE;
    v_movie_count := array_length(v_movies, 1);
    IF v_movie_count IS NULL OR v_movie_count = 0 THEN
        RAISE NOTICE 'No movies found!'; RETURN;
    END IF;

    FOR v_city IN
        SELECT DISTINCT c.id AS city_id, c.name
        FROM cities c JOIN theaters t ON t.city_id = c.id AND t.is_active = TRUE
        JOIN screens sc ON sc.theater_id = t.id AND sc.is_active = TRUE
        ORDER BY c.name
    LOOP
        SELECT array_agg(sc.id ORDER BY sc.id), array_agg(sc.screen_type ORDER BY sc.id)
        INTO v_screens, v_screen_types
        FROM screens sc JOIN theaters t ON sc.theater_id = t.id
        WHERE t.city_id = v_city.city_id AND t.is_active = TRUE AND sc.is_active = TRUE;

        v_screen_count := array_length(v_screens, 1);
        -- How many slots each screen needs: ceil(movies / screens)
        v_slots_per_screen := CEIL(v_movie_count::NUMERIC / v_screen_count);

        FOR v_day IN 0..6 LOOP
            v_show_date := CURRENT_DATE + v_day;

            FOR v_mi IN 1..v_movie_count LOOP
                -- Assign each movie a unique slot: spread across screens, then timeslots
                DECLARE
                    v_assigned INTEGER := v_mi - 1;
                    v_interval INTEGER;  -- minutes between slots
                BEGIN
                    v_si := (v_assigned % v_screen_count) + 1;
                    v_time_offset := v_assigned / v_screen_count;

                    -- Dynamically calculate interval so all movies fit before 23:59
                    -- Available window: 09:00 to 23:59 = 899 minutes
                    -- Need (movies/screens) slots, so interval = window / slots
                    v_interval := GREATEST(30, FLOOR(899.0 / GREATEST(CEIL(v_movie_count::NUMERIC / v_screen_count), 1))::INTEGER);

                    v_start := ('09:00'::TIME + (v_time_offset * (v_interval || ' minutes')::INTERVAL))::TIME;
                    v_end   := (v_start + INTERVAL '25 minutes')::TIME;

                    -- Skip if show would start at or after midnight (safety)
                    CONTINUE WHEN v_start >= '23:45'::TIME;

                    CASE v_screen_types[v_si]
                        WHEN 'IMAX' THEN  v_base:=350; v_prem:=500; v_recl:=800;
                        WHEN 'DOLBY' THEN v_base:=400; v_prem:=550; v_recl:=900;
                        WHEN '4DX' THEN   v_base:=450; v_prem:=600; v_recl:=850;
                        ELSE              v_base:=200; v_prem:=350; v_recl:=600;
                    END CASE;

                    INSERT INTO shows (id, movie_id, screen_id, show_date, start_time, end_time,
                                       base_price, premium_price, recliner_price)
                    VALUES (uuid_generate_v4(), v_movies[v_mi], v_screens[v_si],
                            v_show_date, v_start, v_end, v_base, v_prem, v_recl)
                    ON CONFLICT (screen_id, show_date, start_time) DO NOTHING;

                    v_show_count := v_show_count + 1;
                END;
            END LOOP;
        END LOOP;

        RAISE NOTICE 'City %: % screens, ~% slots/screen/day',
            v_city.name, v_screen_count, v_slots_per_screen;
    END LOOP;

    SELECT COUNT(*) INTO v_inserted FROM shows;
    RAISE NOTICE 'Shows seeding completed! % inserts attempted, % shows in DB.', v_show_count, v_inserted;
END $$;
