INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.mobile_money_option_id
FROM countries c
         JOIN mobile_money_option m ON m.name IN ('AIRTEL', 'MTN', 'TIGO', 'VODAFONE')
WHERE c.acronym = 'GH'
ON CONFLICT DO NOTHING;

INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.mobile_money_option_id
FROM countries c
         JOIN mobile_money_option m ON m.name IN ('FMM', 'WAVE')
WHERE c.acronym IN ('CIV', 'CM')
ON CONFLICT DO NOTHING;

INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.mobile_money_option_id
FROM countries c
         JOIN mobile_money_option m ON m.name IN ('Airtel Kenya (MPX)', 'M-Pesa (MPS)')
WHERE c.acronym = 'KE'
ON CONFLICT DO NOTHING;

INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.mobile_money_option_id
FROM countries c
         JOIN mobile_money_option m ON m.name IN ('EMONEY', 'FREEMONEY', 'ORANGEMONEY', 'WAVE')
WHERE c.acronym = 'SN'
ON CONFLICT DO NOTHING;

INSERT INTO country_mobile_money_options (country_id, mobile_money_option_id)
SELECT c.country_id, m.mobile_money_option_id
FROM countries c
         JOIN mobile_money_option m ON m.name = 'MPS'
WHERE c.acronym IN ('RW', 'UG', 'ZW')
ON CONFLICT DO NOTHING;
