-- ============================================
-- Seed Data: Coupons
-- ============================================

INSERT INTO coupons (id, code, description, discount_type, discount_value, max_discount, min_order_value, valid_from, valid_until, max_uses, current_uses, is_active) VALUES
-- Percentage-based coupons
(uuid_generate_v4(), 'WELCOME50', 'Welcome offer - 50% off on first booking', 'PERCENTAGE', 50.00, 200.00, 300.00,
 NOW() - INTERVAL '30 days', NOW() + INTERVAL '180 days', 10000, 0, TRUE),

(uuid_generate_v4(), 'BMS20', '20% off on all bookings', 'PERCENTAGE', 20.00, 150.00, 200.00,
 NOW() - INTERVAL '10 days', NOW() + INTERVAL '90 days', 5000, 42, TRUE),

(uuid_generate_v4(), 'WEEKEND30', '30% off on weekend shows', 'PERCENTAGE', 30.00, 250.00, 400.00,
 NOW() - INTERVAL '5 days', NOW() + INTERVAL '60 days', 3000, 15, TRUE),

(uuid_generate_v4(), 'IMAX25', '25% off on IMAX shows', 'PERCENTAGE', 25.00, 300.00, 500.00,
 NOW() - INTERVAL '15 days', NOW() + INTERVAL '120 days', 2000, 8, TRUE),

(uuid_generate_v4(), 'FESTIVE40', 'Festival special - 40% off', 'PERCENTAGE', 40.00, 350.00, 500.00,
 NOW() - INTERVAL '7 days', NOW() + INTERVAL '30 days', 1500, 120, TRUE),

-- Fixed-amount coupons
(uuid_generate_v4(), 'FLAT100', 'Flat ₹100 off on bookings above ₹300', 'FIXED', 100.00, NULL, 300.00,
 NOW() - INTERVAL '20 days', NOW() + INTERVAL '90 days', 8000, 230, TRUE),

(uuid_generate_v4(), 'FLAT200', 'Flat ₹200 off on bookings above ₹600', 'FIXED', 200.00, NULL, 600.00,
 NOW() - INTERVAL '10 days', NOW() + INTERVAL '60 days', 4000, 85, TRUE),

(uuid_generate_v4(), 'FLAT500', 'Flat ₹500 off on premium bookings', 'FIXED', 500.00, NULL, 1500.00,
 NOW() - INTERVAL '5 days', NOW() + INTERVAL '45 days', 1000, 12, TRUE),

(uuid_generate_v4(), 'NEWUSER', 'New user special - Flat ₹150 off', 'FIXED', 150.00, NULL, 250.00,
 NOW() - INTERVAL '60 days', NOW() + INTERVAL '365 days', 50000, 1200, TRUE),

-- Expired coupon (for testing)
(uuid_generate_v4(), 'EXPIRED10', 'Expired coupon test', 'PERCENTAGE', 10.00, 100.00, 100.00,
 NOW() - INTERVAL '60 days', NOW() - INTERVAL '1 day', 1000, 500, TRUE),

-- Inactive coupon (for testing)
(uuid_generate_v4(), 'INACTIVE20', 'Inactive coupon test', 'PERCENTAGE', 20.00, 100.00, 100.00,
 NOW() - INTERVAL '5 days', NOW() + INTERVAL '30 days', 1000, 0, FALSE),

-- Maxed-out coupon (for testing)
(uuid_generate_v4(), 'MAXEDOUT', 'Maxed out coupon test', 'FIXED', 100.00, NULL, 200.00,
 NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', 100, 100, TRUE)

ON CONFLICT (code) DO NOTHING;
