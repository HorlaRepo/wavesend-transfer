-- Schema changes for countries table
BEGIN;

-- Add region column to countries table
ALTER TABLE countries ADD COLUMN IF NOT EXISTS region VARCHAR(50);

COMMIT;