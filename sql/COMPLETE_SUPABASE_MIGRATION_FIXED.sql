-- ============================================
-- COMPLETE SUPABASE MIGRATION SCRIPT (FIXED)
-- Apply all user_id changes made today to Supabase
-- Run this in Supabase Dashboard > SQL Editor
-- ============================================

-- ============================================
-- PART 1: Add user_id column to all tables (if not exists)
-- ============================================

-- Members table
ALTER TABLE members ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_members_user_id ON members(user_id);

-- Incomes table
ALTER TABLE incomes ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_incomes_user_id ON incomes(user_id);

-- Expenses table
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON expenses(user_id);

-- Due Collections table
ALTER TABLE due_collections ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_due_collections_user_id ON due_collections(user_id);

-- Due Types table
ALTER TABLE due_types ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_due_types_user_id ON due_types(user_id);

-- Income Types table
ALTER TABLE income_types ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_income_types_user_id ON income_types(user_id);

-- Inventory Items table
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_inventory_items_user_id ON inventory_items(user_id);

-- Damaged Items table
ALTER TABLE damaged_items ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_damaged_items_user_id ON damaged_items(user_id);

-- Rent Items table
ALTER TABLE rent_items ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_rent_items_user_id ON rent_items(user_id);

-- Rents table
ALTER TABLE rents ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_rents_user_id ON rents(user_id);

-- Events table
ALTER TABLE events ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id);

-- Masjids table
ALTER TABLE masjids ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_masjids_user_id ON masjids(user_id);

-- Staff table
ALTER TABLE staff ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_staff_user_id ON staff(user_id);

-- Staff Salaries table
ALTER TABLE staff_salaries ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_staff_salaries_user_id ON staff_salaries(user_id);

-- Committees table
ALTER TABLE committees ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_committees_user_id ON committees(user_id);

-- Houses table
ALTER TABLE houses ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_houses_user_id ON houses(user_id);

-- Marriage Certificates table
ALTER TABLE marriage_certificates ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_marriage_certificates_user_id ON marriage_certificates(user_id);

-- Death Certificates table
ALTER TABLE death_certificates ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_death_certificates_user_id ON death_certificates(user_id);

-- Jamath Certificates table
ALTER TABLE jamath_certificates ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_jamath_certificates_user_id ON jamath_certificates(user_id);

-- Custom Certificates table
ALTER TABLE custom_certificates ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_custom_certificates_user_id ON custom_certificates(user_id);

-- ============================================
-- PART 2: Find the correct user table name
-- ============================================
-- Run this first to see what user/admin tables exist:
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE '%user%' OR table_name LIKE '%admin%';

-- ============================================
-- PART 3: Assign existing records to a user
-- ============================================
-- OPTION 1: If you know the user_id value (e.g., '1' for sam, '2' for rajeesh)
-- Replace '1' with the actual user_id you want to assign records to

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

-- ============================================
-- ALTERNATIVE: If you have a users/auth.users table
-- ============================================
-- Uncomment and modify the section below if you have a users table in Supabase
-- For Supabase Auth, you might use auth.users table:

/*
-- Find user ID from auth.users (if using Supabase Auth)
-- First, check what users exist:
-- SELECT id, email FROM auth.users;

-- Then assign using UUID (replace with actual UUID from auth.users):
UPDATE members SET user_id = 'actual-user-uuid-here' WHERE user_id IS NULL OR user_id = '';
-- ... repeat for other tables
*/

-- ============================================
-- PART 4: Verification Queries
-- ============================================

-- Check which tables have user_id column
SELECT table_name, column_name 
FROM information_schema.columns 
WHERE column_name = 'user_id' 
AND table_schema = 'public'
ORDER BY table_name;

-- Check record counts by user_id (should show all records assigned)
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



