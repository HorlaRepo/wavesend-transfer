-- Make debit_transaction_transaction_id and credit_transaction_transaction_id nullable in the transaction_reference table

-- Drop existing foreign key constraints first (if they exist)
ALTER TABLE IF EXISTS transaction_reference 
    DROP CONSTRAINT IF EXISTS fk_transaction_reference_debit_transaction;

ALTER TABLE IF EXISTS transaction_reference 
    DROP CONSTRAINT IF EXISTS fk_transaction_reference_credit_transaction;

-- Alter columns to make them nullable
ALTER TABLE transaction_reference 
    ALTER COLUMN debit_transaction_transaction_id DROP NOT NULL;

ALTER TABLE transaction_reference 
    ALTER COLUMN credit_transaction_transaction_id DROP NOT NULL;

-- Re-add foreign key constraints (if needed) but with ON DELETE SET NULL
ALTER TABLE IF EXISTS transaction_reference 
    ADD CONSTRAINT fk_transaction_reference_debit_transaction 
    FOREIGN KEY (debit_transaction_transaction_id) 
    REFERENCES transactions(transaction_id) 
    ON DELETE SET NULL;

ALTER TABLE IF EXISTS transaction_reference 
    ADD CONSTRAINT fk_transaction_reference_credit_transaction 
    FOREIGN KEY (credit_transaction_transaction_id) 
    REFERENCES transactions(transaction_id) 
    ON DELETE SET NULL;

-- Add a comment explaining why this change was made
COMMENT ON TABLE transaction_reference IS 'Table storing transaction reference mappings with nullable transaction IDs to support staged transactions like Stripe payments';