-- Base tables without foreign key dependencies
CREATE TABLE IF NOT EXISTS countries (
    country_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    acronym VARCHAR(255),
    currency VARCHAR(255),
    rating INTEGER,
    region VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS payment_method (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS mobile_money_option (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS account_limits (
    id BIGSERIAL PRIMARY KEY,
    verification_level VARCHAR(50),
    daily_sending_limit DECIMAL(19, 2),
    daily_receiving_limit DECIMAL(19, 2),
    daily_withdrawal_limit DECIMAL(19, 2),
    monthly_sending_limit DECIMAL(19, 2),
    monthly_receiving_limit DECIMAL(19, 2),
    monthly_withdrawal_limit DECIMAL(19, 2),
    transaction_min_amount DECIMAL(19, 2),
    transaction_max_amount DECIMAL(19, 2),
    daily_transaction_limit DECIMAL(19, 4),
    max_wallet_balance DECIMAL(19, 4),
    max_deposit_amount DECIMAL(19, 4),
    max_withdrawal_amount DECIMAL(19, 4),
    max_transfer_amount DECIMAL(19, 4),
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_profile_image (
    id BIGSERIAL PRIMARY KEY,
    image_url VARCHAR(1000),
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS security_questions (
    id BIGSERIAL PRIMARY KEY,
    question TEXT,
    answer TEXT,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS kyc_verification (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    id_country VARCHAR(255),
    id_type VARCHAR(255),
    id_number VARCHAR(255),
    expiry_date DATE,
    address_document_url VARCHAR(255),
    id_document_url VARCHAR(255),
    id_rejection_reason VARCHAR(255),
    address_rejection_reason VARCHAR(255),
    address_verification_status VARCHAR(50),
    id_verification_status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS admins (
    admin_id SERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    date_of_birth DATE,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    email VARCHAR(255) UNIQUE,
    account_locked BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS beneficiary_ai_suggestion (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    beneficiary_id BIGINT,
    beneficiary_name VARCHAR(255),
    suggested_amount DECIMAL(19, 2),
    suggestion_text VARCHAR(1000),
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    seen BOOLEAN DEFAULT FALSE,
    dismissed BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    notify_on_send BOOLEAN DEFAULT FALSE,
    notify_on_receive BOOLEAN DEFAULT FALSE,
    notify_on_withdraw BOOLEAN DEFAULT FALSE,
    notify_on_deposit BOOLEAN DEFAULT FALSE,
    notify_on_payment_failure BOOLEAN DEFAULT FALSE,
    notify_on_scheduled_transfers BOOLEAN DEFAULT FALSE,
    notify_on_executed_transfers BOOLEAN DEFAULT FALSE,
    notify_on_cancelled_transfers BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_account_limits (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    verification_level VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS wallet (
    id BIGSERIAL PRIMARY KEY,
    wallet_id VARCHAR(255) UNIQUE,
    balance DECIMAL(19, 2) NOT NULL,
    is_flagged BOOLEAN DEFAULT FALSE,
    currency VARCHAR(255),
    version INTEGER,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);