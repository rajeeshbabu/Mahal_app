-- Fix subscriptions table: Add user_email column if missing
-- Run this in Supabase SQL Editor

-- Step 1: Add user_email column (if it doesn't exist)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'subscriptions' 
        AND column_name = 'user_email'
    ) THEN
        ALTER TABLE subscriptions ADD COLUMN user_email TEXT;
        RAISE NOTICE 'Added user_email column';
    ELSE
        RAISE NOTICE 'user_email column already exists';
    END IF;
END $$;

-- Step 2: Create index for user_email (if it doesn't exist)
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_email ON subscriptions(user_email);

-- Step 3: Update existing records
-- If user_id is already an email (contains @), copy it to user_email
-- Otherwise, set user_email to user_id + '@mahalapp.com'
UPDATE subscriptions 
SET user_email = CASE 
    WHEN user_id LIKE '%@%' THEN user_id
    ELSE user_id || '@mahalapp.com'
END
WHERE user_email IS NULL OR user_email = '';

-- Verification query (optional - uncomment to check)
-- SELECT id, user_id, user_email, status FROM subscriptions LIMIT 10;

