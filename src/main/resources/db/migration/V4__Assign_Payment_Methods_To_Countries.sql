INSERT INTO country_payment_method (country_id, payment_method_id)
SELECT c.country_id, pm.payment_method_id
FROM countries c
         JOIN payment_method pm ON pm.name = 'Bank Account'
WHERE c.acronym = 'NG'
ON CONFLICT DO NOTHING;

INSERT INTO country_payment_method (country_id, payment_method_id)
SELECT c.country_id, pm.payment_method_id
FROM countries c
         JOIN payment_method pm ON pm.name = 'USD (NG DOM Account)'
WHERE c.acronym = 'NG'
ON CONFLICT DO NOTHING;
