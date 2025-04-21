INSERT INTO mobile_money_option (name) VALUES
                                           ('FMM'), ('WAVE'), ('AMOLEMONEY'), ('AIRTEL'), ('MTN'), ('TIGO'), ('VODAFONE'),
                                           ('Airtel Kenya (MPX)'), ('M-Pesa (MPS)'), ('AIRTELMW'), ('EMONEY'), ('FREEMONEY'),
                                           ('ORANGEMONEY')
ON CONFLICT (name) DO NOTHING;
