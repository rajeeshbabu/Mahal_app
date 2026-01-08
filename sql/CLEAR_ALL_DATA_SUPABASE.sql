-- SQL Script: Clear all data from Supabase database (Single Transaction)
-- Run this in Supabase SQL Editor
-- 
-- ⚠️ WARNING: This will delete ALL data from all tables!
-- Make a backup first if you need to preserve any data.

BEGIN;

DELETE FROM sync_queue;
DELETE FROM sync_metadata;
DELETE FROM jamath_certificates;
DELETE FROM custom_certificates;
DELETE FROM students;
DELETE FROM houses;
DELETE FROM committees;
DELETE FROM staff_salaries;
DELETE FROM staff;
DELETE FROM rents;
DELETE FROM rent_items;
DELETE FROM damaged_items;
DELETE FROM inventory_items;
DELETE FROM events;
DELETE FROM masjids;
DELETE FROM due_collections;
DELETE FROM expenses;
DELETE FROM incomes;
DELETE FROM members;
DELETE FROM admins;

COMMIT;

-- Verify tables are empty (optional)
-- SELECT 'admins' as table_name, COUNT(*) as count FROM admins
-- UNION ALL SELECT 'members', COUNT(*) FROM members
-- UNION ALL SELECT 'incomes', COUNT(*) FROM incomes
-- UNION ALL SELECT 'expenses', COUNT(*) FROM expenses;
