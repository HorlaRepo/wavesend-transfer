-- Create users table for JWT authentication
CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone_number VARCHAR(255),
    date_of_birth DATE,
    gender VARCHAR(20),
    enabled BOOLEAN DEFAULT FALSE NOT NULL,
    account_locked BOOLEAN DEFAULT FALSE NOT NULL,
    roles VARCHAR(500) DEFAULT 'ROLE_USER',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

-- Create index on email for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Create index on user_id for joins
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
