INSERT INTO country_payment_method (country_id, payment_method_id)
SELECT c.country_id, pm.payment_method_id
FROM countries c
         JOIN payment_method pm ON pm.name = 'Bank Account'
WHERE c.acronym IN ('US', 'UK', 'DE', 'SA', 'EG', 'KE', 'GH')
ON CONFLICT DO NOTHING;

INSERT INTO country_payment_method (country_id, payment_method_id)
SELECT c.country_id, pm.payment_method_id
FROM countries c
         JOIN payment_method pm ON pm.name = 'Mobile Money'
WHERE c.acronym IN ('KE', 'CIV', 'ML', 'CM', 'SN', 'GH', 'RW', 'UG', 'ZW')
ON CONFLICT DO NOTHING;
