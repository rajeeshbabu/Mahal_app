-- Add subscription records for existing users in SQLite (frontend/mahal.db)
-- First, get your user emails from the admins table:
-- SELECT id, name, full_name FROM admins ORDER BY id;

-- REPLACE THE VALUES BELOW WITH YOUR ACTUAL USER DATA

-- Insert subscription records for your 3 users
-- Format: (user_id, user_email, plan_duration, status, start_date, end_date, razorpay_subscription_id)

INSERT INTO subscriptions (user_id, user_email, plan_duration, status, start_date, end_date, razorpay_subscription_id, created_at, updated_at)
VALUES 
    -- User 1: Replace 'user1@example.com' with actual email from admins table
    ('user1@example.com', 'user1@example.com', 'monthly', 'active', 
     datetime('now'), datetime('now', '+1 month'), 'sub_dummy_1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- User 2: Replace 'user2@example.com' with actual email from admins table
    ('user2@example.com', 'user2@example.com', 'monthly', 'active', 
     datetime('now'), datetime('now', '+1 month'), 'sub_dummy_2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- User 3: Replace 'user3@example.com' with actual email from admins table
    ('user3@example.com', 'user3@example.com', 'yearly', 'active', 
     datetime('now'), datetime('now', '+1 year'), 'sub_dummy_3', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Verification: Check inserted subscriptions
SELECT id, user_id, user_email, plan_duration, status, start_date, end_date FROM subscriptions;

