-- Queries to check subscriptions table in H2 database
-- Run these in H2 Console: http://localhost:8080/h2-console

-- 1. Check if subscriptions table exists and see its structure
SHOW COLUMNS FROM subscriptions;

-- 2. Count total subscriptions
SELECT COUNT(*) AS total_subscriptions FROM subscriptions;

-- 3. View all subscriptions with all columns
SELECT 
    id,
    user_id,
    user_email,
    plan_duration,
    status,
    start_date,
    end_date,
    razorpay_subscription_id,
    created_at,
    updated_at
FROM subscriptions 
ORDER BY created_at DESC;

-- 4. View subscriptions by status
SELECT 
    status,
    COUNT(*) AS count
FROM subscriptions 
GROUP BY status;

-- 5. View active subscriptions only
SELECT 
    id,
    user_id,
    user_email,
    plan_duration,
    status,
    start_date,
    end_date,
    created_at
FROM subscriptions 
WHERE status = 'active'
ORDER BY created_at DESC;

-- 6. View subscriptions by user (grouped by user_email)
SELECT 
    user_email,
    COUNT(*) AS subscription_count,
    MAX(status) AS latest_status,
    MAX(end_date) AS latest_end_date
FROM subscriptions 
WHERE user_email IS NOT NULL
GROUP BY user_email
ORDER BY user_email;

-- 7. Check if user_email column exists (if you get error, the column might not exist yet)
SELECT id, user_id, user_email, status FROM subscriptions LIMIT 5;

-- 8. View recent subscriptions (last 10)
SELECT 
    id,
    user_id,
    user_email,
    plan_duration,
    status,
    start_date,
    end_date,
    created_at
FROM subscriptions 
ORDER BY created_at DESC 
LIMIT 10;

