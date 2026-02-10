-- ============================================
-- Seed Data: Users
-- ============================================
-- Passwords are BCrypt encoded (strength=12)
-- All test users have password: Test@1234

INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, status, email_verified, phone_verified) VALUES
-- Admin user
('a0000000-0000-0000-0000-000000000001', 'admin@bookmyshow.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Admin', 'User', '+919900000001', 'ADMIN', 'ACTIVE', TRUE, TRUE),

-- Theater owners
('a0000000-0000-0000-0000-000000000002', 'pvr.owner@bookmyshow.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Rajesh', 'Sharma', '+919900000002', 'THEATER_OWNER', 'ACTIVE', TRUE, TRUE),

('a0000000-0000-0000-0000-000000000003', 'inox.owner@bookmyshow.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Priya', 'Patel', '+919900000003', 'THEATER_OWNER', 'ACTIVE', TRUE, TRUE),

-- Regular customers
('a0000000-0000-0000-0000-000000000010', 'rahul@example.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Rahul', 'Kumar', '+919900000010', 'CUSTOMER', 'ACTIVE', TRUE, TRUE),

('a0000000-0000-0000-0000-000000000011', 'sneha@example.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Sneha', 'Reddy', '+919900000011', 'CUSTOMER', 'ACTIVE', TRUE, TRUE),

('a0000000-0000-0000-0000-000000000012', 'amit@example.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Amit', 'Singh', '+919900000012', 'CUSTOMER', 'ACTIVE', TRUE, FALSE),

('a0000000-0000-0000-0000-000000000013', 'deepika@example.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Deepika', 'Nair', '+919900000013', 'CUSTOMER', 'ACTIVE', TRUE, TRUE),

('a0000000-0000-0000-0000-000000000014', 'vikram@example.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Vikram', 'Desai', '+919900000014', 'CUSTOMER', 'ACTIVE', FALSE, FALSE),

('a0000000-0000-0000-0000-000000000015', 'ananya@example.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Ananya', 'Iyer', '+919900000015', 'CUSTOMER', 'ACTIVE', TRUE, TRUE),

('a0000000-0000-0000-0000-000000000016', 'karan@example.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Karan', 'Mehta', '+919900000016', 'CUSTOMER', 'ACTIVE', TRUE, TRUE),

('a0000000-0000-0000-0000-000000000017', 'test@example.com',
 '$2a$12$ZpKyTkKjtAbgaxzlkCRyLu4aMkSuhRxcELUntXmLDAUvFEpyxiEga',
 'Test', 'User', '+919900000017', 'CUSTOMER', 'ACTIVE', TRUE, TRUE)

ON CONFLICT (email) DO NOTHING;
