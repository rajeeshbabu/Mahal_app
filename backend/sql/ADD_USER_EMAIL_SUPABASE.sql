-- Add user_email column to subscriptions table in Supabase
-- Run this in Supabase SQL Editor

-- Step 1: Add user_email column if it doesn't exist
ALTER TABLE subscriptions 
ADD COLUMN IF NOT EXISTS user_email TEXT;

-- Step 2: Create index for user_email for better query performance
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_email ON subscriptions(user_email);

-- Step 3: Update existing records: Copy user_id to user_email (since user_id is the email)
UPDATE subscriptions 
SET user_email = user_id 
WHERE user_email IS NULL OR user_email = '';

-- Verification query (optional - uncomment to check)
-- SELECT id, user_id, user_email, status FROM subscriptions LIMIT 10;

