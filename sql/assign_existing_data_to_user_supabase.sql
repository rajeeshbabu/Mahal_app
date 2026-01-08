-- Migration Script: Assign Existing Data to User in Supabase (PostgreSQL)
-- IMPORTANT: Run this script in Supabase Dashboard > SQL Editor
-- 
-- Before running:
-- 1. Find the admin user ID you want to assign data to
-- 2. Replace <USER_ID> with the actual UUID or ID (depending on your schema)
-- 3. If your user_id column stores UUID, use UUID format: '123e4567-e89b-12d3-a456-426614174000'
-- 4. If your user_id column stores TEXT (admin ID), use the numeric ID as text: '1'

-- ============================================
-- STEP 1: Find the user ID you want to use
-- ============================================
-- Run this first to see all admin users (adjust table name if different):
-- SELECT id, name, full_name FROM admins;

-- If user_id is stored as UUID, you might need to convert:
-- SELECT id::text, name, full_name FROM admins;

-- ============================================
-- STEP 2: Replace <USER_ID> with actual ID
-- ============================================
-- After finding the user ID, update all the UPDATE statements below
-- IMPORTANT: Make sure the user_id column exists in all tables first!
-- Run: sql/migrate_add_user_id_supabase.sql if you haven't already

-- Assign existing records to user (replace <USER_ID> with actual admin ID)
-- Update members
UPDATE members SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update incomes
UPDATE incomes SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update expenses
UPDATE expenses SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update due_collections
UPDATE due_collections SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update masjids
UPDATE masjids SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update committees
UPDATE committees SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update staff
UPDATE staff SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update staff_salaries
UPDATE staff_salaries SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update income_types
UPDATE income_types SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update due_types
UPDATE due_types SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update events
UPDATE events SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update inventory_items
UPDATE inventory_items SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update houses
UPDATE houses SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update damaged_items
UPDATE damaged_items SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update rent_items
UPDATE rent_items SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update rents
UPDATE rents SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- Update certificate tables
UPDATE marriage_certificates SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';
UPDATE death_certificates SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';
UPDATE jamath_certificates SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';
UPDATE custom_certificates SET user_id = '<USER_ID>' WHERE user_id IS NULL OR user_id = '';

-- ============================================
-- If user_id is UUID type, use this format:
-- ============================================
-- UPDATE members SET user_id = '123e4567-e89b-12d3-a456-426614174000'::uuid 
-- WHERE user_id IS NULL;
-- 
-- Or if you need to convert from admins table:
-- UPDATE members SET user_id = (SELECT id::text FROM admins WHERE name = 'sam' LIMIT 1)
-- WHERE user_id IS NULL;

-- ============================================
-- If user_id is TEXT type (stores admin ID as text):
-- ============================================
-- UPDATE members SET user_id = (SELECT id::text FROM admins WHERE name = 'sam' LIMIT 1)
-- WHERE user_id IS NULL OR user_id = '';

-- ============================================
-- VERIFICATION: Check if migration worked
-- ============================================
-- After running the updates, verify by checking record counts:
-- SELECT user_id, COUNT(*) FROM members GROUP BY user_id;
-- SELECT user_id, COUNT(*) FROM incomes GROUP BY user_id;
-- SELECT user_id, COUNT(*) FROM masjids GROUP BY user_id;
-- etc.

-- Check for records still without user_id:
-- SELECT COUNT(*) FROM members WHERE user_id IS NULL OR user_id = '';
-- SELECT COUNT(*) FROM incomes WHERE user_id IS NULL OR user_id = '';



