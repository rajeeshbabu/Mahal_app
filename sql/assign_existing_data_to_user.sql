-- Migration Script: Assign Existing Data to User
-- IMPORTANT: Before running this script, find the admin ID for the user you want to assign data to
-- Run this query first to find user IDs:
-- SELECT id, name, full_name FROM admins;

-- Replace <USER_ID> below with the actual admin ID (e.g., 1 for sam, or the ID of user1)
-- Example: If "sam" has id=1, replace <USER_ID> with 1
-- Example: If "user1" has id=2, replace <USER_ID> with 2

-- ============================================
-- STEP 1: Find the user ID you want to use
-- ============================================
-- Run this first to see all admin users:
-- SELECT id, name, full_name FROM admins;

-- ============================================
-- STEP 2: Replace <USER_ID> with actual ID
-- ============================================
-- After finding the user ID, update all the UPDATE statements below

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
-- VERIFICATION: Check if migration worked
-- ============================================
-- After running the updates, verify by checking record counts:
-- SELECT user_id, COUNT(*) FROM members GROUP BY user_id;
-- SELECT user_id, COUNT(*) FROM incomes GROUP BY user_id;
-- etc.



