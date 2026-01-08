-- Add user_id column to masjids table if it doesn't exist
-- Run this in Supabase SQL Editor if the column is missing

-- Add user_id column
ALTER TABLE masjids ADD COLUMN IF NOT EXISTS user_id TEXT;

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_masjids_user_id ON masjids(user_id);

-- Assign existing record to user_id = '1' (change to '2' if needed)
UPDATE masjids SET user_id = '1' WHERE user_id IS NULL OR user_id = '';

-- Verify
SELECT id, user_id, name, abbreviation, address 
FROM masjids 
ORDER BY id;
