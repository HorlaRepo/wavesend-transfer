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
