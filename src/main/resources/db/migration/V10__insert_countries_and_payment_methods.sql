-- Data initialization for countries, payment methods and their relationships
BEGIN;

-- Country data initialization
INSERT INTO countries (rating, acronym, currency, name) VALUES
    (3, 'NG', 'NGN', 'Nigeria'),
    (5, 'US', 'USD', 'United States'),
    (3, 'SA', 'ZAR', 'South Africa'),
    (3, 'EG', 'EGP', 'Egypt'),
    (3, 'KE', 'KES', 'Kenya'),
    (3, 'GH', 'GHS', 'Ghana'),
    (3, 'CIV', 'XOF', 'Ivory Coast'),
    (3, 'ML', 'XOF', 'Mali'),
    (3, 'CM', 'XAF', 'Cameroon'),
    (3, 'SN', 'XOF', 'Senegal'),
    (3, 'RW', 'RWF', 'Rwanda'),
    (3, 'UG', 'UGX', 'Uganda'),
    (3, 'ZW', 'ZMW', 'Zimbabwe'),
    (5, 'UK', 'GBP', 'United Kingdom'),
    (5, 'DE', 'EUR', 'Germany')
ON CONFLICT (name) DO NOTHING;

-- Update region data for countries
UPDATE countries SET region =
    CASE
        WHEN name IN ('Nigeria', 'South Africa', 'Egypt', 'Kenya', 'Ghana', 'Ivory Coast', 
                     'Mali', 'Cameroon', 'Senegal', 'Rwanda', 'Uganda', 'Zimbabwe') THEN 'Africa'
        WHEN name IN ('United Kingdom', 'Germany') THEN 'EU'
        WHEN name = 'United States' THEN 'US'
    END;

-- Payment method initialization
INSERT INTO payment_method (name, created_date)
VALUES 
    ('Mobile Money', NOW()), 
    ('Bank Account', NOW()), 
    ('USD (NG DOM Account)', NOW())
ON CONFLICT (name) DO NOTHING;

-- Associate bank accounts with Nigeria
INSERT INTO country_payment_method (country_id, payment_method_id)
SELECT c.country_id, pm.id
FROM countries c
JOIN payment_method pm ON pm.name = 'Bank Account'
WHERE c.acronym = 'NG'
ON CONFLICT (country_id, payment_method_id) DO NOTHING;

-- Associate USD DOM accounts with Nigeria
INSERT INTO country_payment_method (country_id, payment_method_id)
SELECT c.country_id, pm.id
FROM countries c
JOIN payment_method pm ON pm.name = 'USD (NG DOM Account)'
WHERE c.acronym = 'NG'
ON CONFLICT (country_id, payment_method_id) DO NOTHING;

-- Associate bank accounts with developed countries and some African countries
INSERT INTO country_payment_method (country_id, payment_method_id)
SELECT c.country_id, pm.id
FROM countries c
JOIN payment_method pm ON pm.name = 'Bank Account'
WHERE c.acronym IN ('US', 'UK', 'DE', 'SA', 'EG', 'KE', 'GH')
ON CONFLICT (country_id, payment_method_id) DO NOTHING;

-- Associate mobile money with various African countries
INSERT INTO country_payment_method (country_id, payment_method_id)
SELECT c.country_id, pm.id
FROM countries c
JOIN payment_method pm ON pm.name = 'Mobile Money'
WHERE c.acronym IN ('KE', 'CIV', 'ML', 'CM', 'SN', 'GH', 'RW', 'UG', 'ZW')
ON CONFLICT (country_id, payment_method_id) DO NOTHING;

-- Mobile money options initialization
INSERT INTO mobile_money_option (name) VALUES
    ('FMM'), ('WAVE'), ('AMOLEMONEY'), ('AIRTEL'), ('MTN'), ('TIGO'), ('VODAFONE'),
    ('Airtel Kenya (MPX)'), ('M-Pesa (MPS)'), ('AIRTELMW'), ('EMONEY'), ('FREEMONEY'),
    ('ORANGEMONEY')
ON CONFLICT (name) DO NOTHING;

-- Associate mobile money options with Ghana
INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.id
FROM countries c
JOIN mobile_money_option m ON m.name IN ('AIRTEL', 'MTN', 'TIGO', 'VODAFONE')
WHERE c.acronym = 'GH'
ON CONFLICT (country_id, mobile_money_option_id) DO NOTHING;

-- Associate mobile money options with Ivory Coast and Cameroon
INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.id
FROM countries c
JOIN mobile_money_option m ON m.name IN ('FMM', 'WAVE')
WHERE c.acronym IN ('CIV', 'CM')
ON CONFLICT (country_id, mobile_money_option_id) DO NOTHING;

-- Associate mobile money options with Kenya
INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.id
FROM countries c
JOIN mobile_money_option m ON m.name IN ('Airtel Kenya (MPX)', 'M-Pesa (MPS)')
WHERE c.acronym = 'KE'
ON CONFLICT (country_id, mobile_money_option_id) DO NOTHING;

-- Associate mobile money options with Senegal
INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.id
FROM countries c
JOIN mobile_money_option m ON m.name IN ('EMONEY', 'FREEMONEY', 'ORANGEMONEY', 'WAVE')
WHERE c.acronym = 'SN'
ON CONFLICT (country_id, mobile_money_option_id) DO NOTHING;

-- Associate M-Pesa with Rwanda, Uganda, and Zimbabwe
-- Fix: Use full name 'M-Pesa (MPS)' instead of just 'MPS'
INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.id
FROM countries c
JOIN mobile_money_option m ON m.name = 'M-Pesa (MPS)'
WHERE c.acronym IN ('RW', 'UG', 'ZW')
ON CONFLICT (country_id, mobile_money_option_id) DO NOTHING;

COMMIT;