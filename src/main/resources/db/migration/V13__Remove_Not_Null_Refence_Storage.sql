-- Make transaction reference columns nullable

-- Alter columns to make them nullable
ALTER TABLE transaction_reference 
    ALTER COLUMN debit_transaction_transaction_id DROP NOT NULL;

ALTER TABLE transaction_reference 
    ALTER COLUMN credit_transaction_transaction_id DROP NOT NULL;