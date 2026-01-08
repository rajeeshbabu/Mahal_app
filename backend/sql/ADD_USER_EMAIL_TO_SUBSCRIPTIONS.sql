-- Add user_email column to existing subscriptions table
-- Run this in Supabase SQL Editor if table already exists

ALTER TABLE subscriptions 
ADD COLUMN IF NOT EXISTS user_email TEXT;

-- Create index for user_email for better query performance
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_email ON subscriptions(user_email);

-- Update existing records: If user_id is already an email, copy it to user_email
-- If user_id is not an email, set user_email to user_id + '@mahalapp.com'
UPDATE subscriptions 
SET user_email = CASE 
    WHEN user_id LIKE '%@%' THEN user_id
    ELSE user_id || '@mahalapp.com'
END
WHERE user_email IS NULL;

