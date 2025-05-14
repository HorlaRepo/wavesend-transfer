-- Tables with foreign key dependencies
CREATE TABLE IF NOT EXISTS card (
    id BIGSERIAL PRIMARY KEY,
    card_type VARCHAR(255),
    card_number VARCHAR(255) UNIQUE,
    expiry_date VARCHAR(255),
    cvv VARCHAR(255),
    card_name VARCHAR(255),
    is_locked BOOLEAN DEFAULT FALSE,
    pin VARCHAR(255),
    wallet_id BIGINT,
    FOREIGN KEY (wallet_id) REFERENCES wallet(id)
);

CREATE TABLE IF NOT EXISTS user_beneficiaries (
    user_id VARCHAR(255) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS user_beneficiary (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES user_beneficiaries(user_id)
);

CREATE TABLE IF NOT EXISTS daily_transaction_totals (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    UNIQUE(user_id, date)
);

CREATE TABLE IF NOT EXISTS bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    bank_name VARCHAR(255),
    account_number VARCHAR(255),
    account_name VARCHAR(255),
    account_type VARCHAR(255),
    currency VARCHAR(255),
    bank_country VARCHAR(255),
    bank_code VARCHAR(255),
    region VARCHAR(255),
    swift_code VARCHAR(255),
    routing_number VARCHAR(255),
    beneficiary_name VARCHAR(255),
    beneficiary_address VARCHAR(255),
    beneficiary_country VARCHAR(255),
    postal_code VARCHAR(255),
    street_number VARCHAR(255),
    street_name VARCHAR(255),
    city VARCHAR(255),
    payment_method VARCHAR(255),
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS scheduled_transfers (
    id BIGSERIAL PRIMARY KEY,
    sender_email VARCHAR(255) NOT NULL,
    receiver_email VARCHAR(255) NOT NULL,
    amount DECIMAL(20, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    scheduled_date_time TIMESTAMP NOT NULL,
    executed_at TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    recurrence_type VARCHAR(50) NOT NULL,
    recurrence_end_date TIMESTAMP,
    total_occurrences INTEGER,
    current_occurrence INTEGER,
    parent_transfer_id BIGINT,
    processed BOOLEAN,
    processed_date_time TIMESTAMP,
    retry_count INTEGER,
    last_retry_date_time TIMESTAMP,
    failure_reason VARCHAR(255),
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id SERIAL PRIMARY KEY,
    provider_id VARCHAR(255),
    version BIGINT,
    amount DECIMAL(19, 2) NOT NULL,
    refundable_amount DECIMAL(19, 2) DEFAULT 0,
    mtcn VARCHAR(255) UNIQUE,
    current_status VARCHAR(255),
    transaction_date TIMESTAMP NOT NULL,
    reference_number VARCHAR(255),
    description VARCHAR(255),
    narration VARCHAR(255),
    failure_reason VARCHAR(255),
    completed_at TIMESTAMP,
    fee DOUBLE PRECISION,
    operation VARCHAR(50),
    sending_method VARCHAR(50),
    delivery_method VARCHAR(50),
    wallet_id BIGINT,
    transaction_type VARCHAR(50),
    session_id VARCHAR(255),
    security_question_id BIGINT,
    flagged BOOLEAN DEFAULT FALSE,
    source VARCHAR(50),
    refund_status VARCHAR(50),
    refund_date TIMESTAMP,
    FOREIGN KEY (wallet_id) REFERENCES wallet(id),
    FOREIGN KEY (security_question_id) REFERENCES security_questions(id)
);

CREATE TABLE IF NOT EXISTS flagged_transaction_reason (
    id BIGSERIAL PRIMARY KEY,
    reason VARCHAR(255) NOT NULL,
    transaction_id INTEGER NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

CREATE TABLE IF NOT EXISTS refund_impact_records (
    id BIGSERIAL PRIMARY KEY,
    deposit_transaction_id INTEGER,
    impact_type VARCHAR(50),
    impact_amount DECIMAL(19, 2),
    previous_refundable_amount DECIMAL(19, 2),
    new_refundable_amount DECIMAL(19, 2),
    impact_date TIMESTAMP,
    related_transfer_amount DECIMAL(19, 2),
    notes VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS transaction_status (
    id SERIAL PRIMARY KEY,
    status VARCHAR(255),
    note VARCHAR(255),
    status_date TIMESTAMP,
    admin_id INTEGER,
    transaction_id INTEGER,
    FOREIGN KEY (admin_id) REFERENCES admins(admin_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

CREATE TABLE IF NOT EXISTS transaction_reference (
    id BIGSERIAL PRIMARY KEY,
    debit_transaction_id INTEGER NULL REFERENCES transactions(transaction_id),
    credit_transaction_id INTEGER NULL REFERENCES transactions(transaction_id),
    debit_transaction_transaction_id INTEGER NULL,
    credit_transaction_transaction_id INTEGER NULL,
    reference_number VARCHAR(255)
);