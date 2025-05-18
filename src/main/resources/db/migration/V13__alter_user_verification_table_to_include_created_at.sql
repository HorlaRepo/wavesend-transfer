-- First check if the created_date column exists, then drop it
DO $$ 
BEGIN
    IF EXISTS (
        SELECT FROM information_schema.columns 
        WHERE table_name = 'user_account_limits' AND column_name = 'created_date'
    ) THEN
        ALTER TABLE user_account_limits DROP COLUMN created_date;
    END IF;
END $$;

-- Ensure the created_at column has the appropriate comment
COMMENT ON COLUMN user_account_limits.created_at IS 'Timestamp when the account limit was created';