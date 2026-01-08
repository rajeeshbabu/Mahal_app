-- Check if user_id column exists in masjids table
-- Run this in Supabase SQL Editor

-- 1. Check if user_id column exists
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'masjids' 
AND column_name = 'user_id'
AND table_schema = 'public';

-- 2. Show all columns in masjids table
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'masjids' 
AND table_schema = 'public'
ORDER BY ordinal_position;

-- 3. Check current data with user_id
SELECT id, user_id, name, abbreviation, address 
FROM masjids 
ORDER BY id;

-- 4. Count records by user_id
SELECT user_id, COUNT(*) as count 
FROM masjids 
GROUP BY user_id;
