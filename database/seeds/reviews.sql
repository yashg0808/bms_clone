-- ============================================
-- Seed Data: Reviews & Audit Log
-- ============================================
-- Requires: movies to be seeded first.
-- Reviews use placeholder user UUIDs (no FK constraint).

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

    v_reviews TEXT[] := ARRAY[
        'Absolutely loved it! Must watch in IMAX.',
        'Good movie overall but the second half dragged a bit.',
        'Amazing performances by the entire cast. Gripping from start to finish.',
        'Worth every penny. Go watch it on the big screen!',
        'Decent entertainer, nothing groundbreaking though.',
        'One of the best movies of the year. Highly recommended!',
        'Bit overhyped but still enjoyable. Good visual effects.',
        'Brilliant direction and screenplay. A cinematic masterpiece.',
        'Fun family entertainer. Kids loved it too!',
        'The action sequences were mind-blowing. Story could have been better.',
        'Incredible soundtrack and visuals. Story is engaging throughout.',
        'A bit predictable but still entertaining. Good performances.',
        'Perfect weekend watch with friends and family.',
        'The climax was absolutely phenomenal!',
        'Good attempt but didn''t live up to the prequel.',
        'Edge-of-the-seat thriller. Keeps you hooked till the end.'
    ];

    v_ratings INTEGER[] := ARRAY[9, 7, 8, 9, 6, 10, 7, 9, 8, 7, 8, 6, 7, 9, 5, 8];

    v_movie RECORD;
    v_user_idx INTEGER;
    v_review_idx INTEGER := 1;
    v_movie_count INTEGER := 0;
BEGIN
    FOR v_movie IN
        SELECT id FROM movies WHERE is_active = TRUE ORDER BY release_date DESC LIMIT 20
    LOOP
        v_movie_count := v_movie_count + 1;

        -- Each movie gets 3-5 reviews from different users
        FOR v_user_idx IN 1..LEAST(3 + (v_movie_count % 3), array_length(v_user_ids, 1)) LOOP
            INSERT INTO reviews (user_id, movie_id, rating, review_text, is_approved)
            VALUES (
                v_user_ids[v_user_idx],
                v_movie.id,
                v_ratings[((v_review_idx - 1) % array_length(v_ratings, 1)) + 1],
                v_reviews[((v_review_idx - 1) % array_length(v_reviews, 1)) + 1],
                -- Approve most reviews, leave some unapproved for testing
                CASE WHEN v_review_idx % 7 = 0 THEN FALSE ELSE TRUE END
            )
            ON CONFLICT (user_id, movie_id) DO NOTHING;

            v_review_idx := v_review_idx + 1;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Reviews seeding completed! Created % reviews.', v_review_idx - 1;

    -- ============================================
    -- Seed some Audit Log entries
    -- ============================================
    INSERT INTO audit_log (entity_type, entity_id, action, new_values, performed_by, ip_address) VALUES
    ('MOVIE', (SELECT id FROM movies LIMIT 1), 'CREATE',
     '{"title":"Sample Movie","language":"Hindi"}',
     NULL, '127.0.0.1'),

    ('BOOKING', 'a0000000-0000-0000-0000-000000000010', 'CREATE',
     '{"booking_number":"BMS-SEED-0001","status":"CONFIRMED"}',
     NULL, '192.168.1.100'),

    ('BOOKING', 'a0000000-0000-0000-0000-000000000014', 'CREATE',
     '{"booking_number":"BMS-SEED-0002","status":"CONFIRMED"}',
     NULL, '10.0.0.55');

    -- Add audit entries for some bookings
    INSERT INTO audit_log (entity_type, entity_id, action, new_values, performed_by, ip_address)
    SELECT 'BOOKING', b.id, 'CREATE',
           json_build_object('booking_number', b.booking_number, 'status', b.status::TEXT, 'final_amount', b.final_amount)::JSONB,
           NULL, '192.168.1.' || (ROW_NUMBER() OVER () + 100)::TEXT
    FROM bookings b
    LIMIT 10;

    RAISE NOTICE 'Audit log seeding completed!';
END $$;
