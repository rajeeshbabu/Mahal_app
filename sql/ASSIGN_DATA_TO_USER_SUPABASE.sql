-- ============================================
-- Assign Existing Data to User in Supabase
-- Run this AFTER the ALTER TABLE statements have completed
-- ============================================
-- The ALTER TABLE statements already ran successfully
-- Now we just need to assign existing records to a user
-- 
-- IMPORTANT: Change '1' to the user_id you want to assign to
-- - '1' = sam
-- - '2' = rajeesh  
-- - '3' = revu
-- ============================================

-- Assign all existing records to user_id = '1' (sam)
-- Change '1' to '2' for rajeesh, or '3' for revu if needed

UPDATE members SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE incomes SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE expenses SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE due_collections SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE masjids SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE committees SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE staff SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE staff_salaries SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE income_types SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE due_types SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE events SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE inventory_items SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE houses SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE damaged_items SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE rent_items SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE rents SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE marriage_certificates SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE death_certificates SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE jamath_certificates SET user_id = '1' WHERE user_id IS NULL OR user_id = '';
UPDATE custom_certificates SET user_id = '1' WHERE user_id IS NULL OR user_id = '';

-- Verification: Check if migration worked
SELECT 'members' as table_name, user_id, COUNT(*) as count FROM members GROUP BY user_id
UNION ALL
SELECT 'incomes', user_id, COUNT(*) FROM incomes GROUP BY user_id
UNION ALL
SELECT 'expenses', user_id, COUNT(*) FROM expenses GROUP BY user_id
UNION ALL
SELECT 'masjids', user_id, COUNT(*) FROM masjids GROUP BY user_id
UNION ALL
SELECT 'committees', user_id, COUNT(*) FROM committees GROUP BY user_id
UNION ALL
SELECT 'staff', user_id, COUNT(*) FROM staff GROUP BY user_id
UNION ALL
SELECT 'events', user_id, COUNT(*) FROM events GROUP BY user_id
UNION ALL
SELECT 'inventory_items', user_id, COUNT(*) FROM inventory_items GROUP BY user_id
ORDER BY table_name, user_id;

-- Check for records still without user_id (should all be 0)
SELECT 
    'members' as table_name,
    COUNT(*) as records_without_user_id 
FROM members 
WHERE user_id IS NULL OR user_id = ''
UNION ALL
SELECT 'incomes', COUNT(*) FROM incomes WHERE user_id IS NULL OR user_id = ''
UNION ALL
SELECT 'masjids', COUNT(*) FROM masjids WHERE user_id IS NULL OR user_id = ''
UNION ALL
SELECT 'committees', COUNT(*) FROM committees WHERE user_id IS NULL OR user_id = ''
ORDER BY table_name;



