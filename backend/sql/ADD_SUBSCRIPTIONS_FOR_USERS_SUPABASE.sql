-- Add subscription records for existing users in Supabase
-- First, get your user emails (they should match the emails in your local admins table)

-- REPLACE THE VALUES BELOW WITH YOUR ACTUAL USER DATA
-- Get user emails from your admins table: SELECT name FROM admins ORDER BY id;

-- Insert subscription records for your 3 users
INSERT INTO subscriptions (user_id, user_email, plan_duration, status, start_date, end_date, razorpay_subscription_id, created_at, updated_at)
VALUES 
    -- User 1: Replace 'user1@example.com' with actual email
    ('user1@example.com', 'user1@example.com', 'monthly', 'active', 
     NOW(), NOW() + INTERVAL '1 month', 'sub_dummy_1', NOW(), NOW()),
    
    -- User 2: Replace 'user2@example.com' with actual email
    ('user2@example.com', 'user2@example.com', 'monthly', 'active', 
     NOW(), NOW() + INTERVAL '1 month', 'sub_dummy_2', NOW(), NOW()),
    
    -- User 3: Replace 'user3@example.com' with actual email
    ('user3@example.com', 'user3@example.com', 'yearly', 'active', 
     NOW(), NOW() + INTERVAL '1 year', 'sub_dummy_3', NOW(), NOW())
ON CONFLICT (razorpay_subscription_id) DO NOTHING;

-- Verification: Check inserted subscriptions
SELECT id, user_id, user_email, plan_duration, status, start_date, end_date FROM subscriptions ORDER BY created_at DESC;

