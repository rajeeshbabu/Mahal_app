-- Quick Migration: Assign All Existing Data to "sam" in Supabase
-- Run this in Supabase Dashboard > SQL Editor
-- This automatically finds "sam" by full_name and assigns all existing records to that user

-- Assign existing records to "sam" (finds user automatically by full_name)
UPDATE members SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE incomes SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE expenses SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE due_collections SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE masjids SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE committees SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE staff SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE staff_salaries SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE income_types SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE due_types SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE events SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE inventory_items SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE houses SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE damaged_items SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE rent_items SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE rents SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE marriage_certificates SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE death_certificates SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE jamath_certificates SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE custom_certificates SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';

-- Verify the migration
SELECT 'members' as table_name, user_id, COUNT(*) as count FROM members GROUP BY user_id
UNION ALL
SELECT 'incomes', user_id, COUNT(*) FROM incomes GROUP BY user_id
UNION ALL
SELECT 'masjids', user_id, COUNT(*) FROM masjids GROUP BY user_id
ORDER BY table_name, user_id;



