-- First add the region column if it doesn't exist
ALTER TABLE countries ADD COLUMN IF NOT EXISTS region VARCHAR(50);

-- Update region for each country based on its name
UPDATE countries SET region =
     CASE
         WHEN name IN ('Nigeria', 'South Africa', 'Egypt', 'Kenya', 'Ghana', 'Ivory Coast', 'Mali', 'Cameroon', 'Senegal', 'Rwanda', 'Uganda', 'Zimbabwe') THEN 'Africa'
         WHEN name IN ('United Kingdom', 'Germany') THEN 'EU'
         WHEN name = 'United States' THEN 'US'
         END;