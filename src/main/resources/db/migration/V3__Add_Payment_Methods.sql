INSERT INTO payment_method (name)
VALUES ('Mobile Money'), ('Bank Account'), ('USD (NG DOM Account)')
ON CONFLICT (name) DO NOTHING;
