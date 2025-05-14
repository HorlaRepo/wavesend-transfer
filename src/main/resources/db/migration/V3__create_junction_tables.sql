-- Junction tables for many-to-many relationships
CREATE TABLE IF NOT EXISTS country_mobile_money_options (
    country_id BIGINT NOT NULL,
    mobile_money_option_id BIGINT NOT NULL,
    PRIMARY KEY (country_id, mobile_money_option_id),
    FOREIGN KEY (country_id) REFERENCES countries(country_id),
    FOREIGN KEY (mobile_money_option_id) REFERENCES mobile_money_option(id)
);

CREATE TABLE IF NOT EXISTS country_payment_method (
    country_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    PRIMARY KEY (country_id, payment_method_id),
    FOREIGN KEY (country_id) REFERENCES countries(country_id),
    FOREIGN KEY (payment_method_id) REFERENCES payment_method(id)
);

-- Create indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_wallet_created_by ON wallet(created_by);
CREATE INDEX IF NOT EXISTS idx_transaction_wallet_id ON transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_transaction_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transaction_status ON transactions(current_status);
CREATE INDEX IF NOT EXISTS idx_transaction_type ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_kyc_user_id ON kyc_verification(user_id);
CREATE INDEX IF NOT EXISTS idx_beneficiary_user_id ON beneficiary_ai_suggestion(user_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_transfers_sender ON scheduled_transfers(sender_email);
CREATE INDEX IF NOT EXISTS idx_scheduled_transfers_status ON scheduled_transfers(status);
CREATE INDEX IF NOT EXISTS idx_daily_transaction_user_date ON daily_transaction_totals(user_id, date);