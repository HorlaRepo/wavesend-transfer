-- Insert default account limits only if they don't exist
INSERT INTO account_limits (verification_level, daily_transaction_limit, max_wallet_balance, max_deposit_amount, max_withdrawal_amount, max_transfer_amount, created_date) 
SELECT 'UNVERIFIED', 200.0000, 500.0000, 100.0000, 50.0000, 100.0000, NOW()
WHERE NOT EXISTS (SELECT 1 FROM account_limits WHERE verification_level = 'UNVERIFIED');

INSERT INTO account_limits (verification_level, daily_transaction_limit, max_wallet_balance, max_deposit_amount, max_withdrawal_amount, max_transfer_amount, created_date) 
SELECT 'EMAIL_VERIFIED', 500.0000, 1000.0000, 300.0000, 200.0000, 500.0000, NOW()
WHERE NOT EXISTS (SELECT 1 FROM account_limits WHERE verification_level = 'EMAIL_VERIFIED');

INSERT INTO account_limits (verification_level, daily_transaction_limit, max_wallet_balance, max_deposit_amount, max_withdrawal_amount, max_transfer_amount, created_date) 
SELECT 'ID_VERIFIED', 5000.0000, 10000.0000, 3000.0000, 2000.0000, 5000.0000, NOW()
WHERE NOT EXISTS (SELECT 1 FROM account_limits WHERE verification_level = 'ID_VERIFIED');

INSERT INTO account_limits (verification_level, daily_transaction_limit, max_wallet_balance, max_deposit_amount, max_withdrawal_amount, max_transfer_amount, created_date) 
SELECT 'FULLY_VERIFIED', NULL, NULL, NULL, NULL, NULL, NOW()
WHERE NOT EXISTS (SELECT 1 FROM account_limits WHERE verification_level = 'FULLY_VERIFIED');