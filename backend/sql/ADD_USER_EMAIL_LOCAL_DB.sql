-- Add user_email column to subscriptions table in local database (H2/MySQL)
-- Run this if you're using MySQL or need to manually update H2

-- For H2 Database:
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS user_email VARCHAR(255);

-- For MySQL Database:
-- ALTER TABLE subscriptions ADD COLUMN user_email VARCHAR(255);

-- Update existing records: Copy user_id to user_email (since user_id is the email)
UPDATE subscriptions 
SET user_email = user_id 
WHERE user_email IS NULL OR user_email = '';

