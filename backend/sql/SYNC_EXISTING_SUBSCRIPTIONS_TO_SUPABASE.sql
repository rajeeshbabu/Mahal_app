-- Manual SQL script to sync existing subscriptions from H2 to Supabase
-- STEP 1: Get subscriptions from H2 database (run in H2 Console)
-- STEP 2: Use the data below to insert into Supabase

-- ========================================
-- STEP 1: Export from H2 Console
-- ========================================
-- Run this in H2 Console (http://localhost:8080/h2-console):
-- JDBC URL: jdbc:h2:file:./data/mahal_db

SELECT 
    'INSERT INTO subscriptions (user_id, user_email, plan_duration, status, start_date, end_date, razorpay_subscription_id, created_at, updated_at) VALUES (''' ||
    user_id || ''', ''' ||
    COALESCE(user_email, user_id) || ''', ''' ||
    plan_duration || ''', ''' ||
    status || ''', ' ||
    CASE WHEN start_date IS NOT NULL THEN '''' || start_date || '''' ELSE 'NULL' END || ', ' ||
    CASE WHEN end_date IS NOT NULL THEN '''' || end_date || '''' ELSE 'NULL' END || ', ''' ||
    COALESCE(razorpay_subscription_id, 'sub_manual_' || id) || ''', ''' ||
    created_at || ''', ''' ||
    updated_at || ''') ON CONFLICT (razorpay_subscription_id) DO NOTHING;' AS insert_sql
FROM subscriptions
ORDER BY id;

-- Copy the output SQL statements and run them in Supabase SQL Editor

-- ========================================
-- STEP 2: Alternative - Manual Insert Template
-- ========================================
-- Or manually create INSERT statements using this template:

-- Replace the values below with actual data from your H2 database:

INSERT INTO subscriptions (user_id, user_email, plan_duration, status, start_date, end_date, razorpay_subscription_id, created_at, updated_at)
VALUES 
    ('user1@example.com', 'user1@example.com', 'monthly', 'active', 
     '2025-01-01 10:00:00', '2025-02-01 10:00:00', 'sub_dummy_1', NOW(), NOW()),
    ('user2@example.com', 'user2@example.com', 'yearly', 'active', 
     '2025-01-01 11:00:00', '2026-01-01 11:00:00', 'sub_dummy_2', NOW(), NOW())
ON CONFLICT (razorpay_subscription_id) DO NOTHING;

-- ========================================
-- STEP 3: Verification
-- ========================================
-- After inserting, verify in Supabase:

SELECT id, user_id, user_email, plan_duration, status, created_at 
FROM subscriptions 
ORDER BY created_at DESC;


