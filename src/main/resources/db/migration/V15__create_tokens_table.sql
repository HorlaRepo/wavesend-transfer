-- Create tokens table for email verification and password reset
CREATE TABLE IF NOT EXISTS tokens (
    token_id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    token_type VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    validated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create index on token for fast lookups
CREATE INDEX IF NOT EXISTS idx_tokens_token ON tokens(token);

-- Create index on user_id for finding user's tokens
CREATE INDEX IF NOT EXISTS idx_tokens_user_id ON tokens(user_id);

-- Create index on token_type for filtering by type
CREATE INDEX IF NOT EXISTS idx_tokens_token_type ON tokens(token_type);
