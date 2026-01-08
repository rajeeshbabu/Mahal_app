-- SQL Script: Migrate Admins from SQLite to Supabase
-- 
-- IMPORTANT: This script provides INSERT statements to copy admin data from SQLite to Supabase.
-- 
-- Steps:
-- 1. First, export your admin data from SQLite (see instructions below)
-- 2. Replace the sample INSERT statements below with your actual data
-- 3. Run this script in Supabase SQL Editor
--
-- ============================================
-- STEP 1: Export Data from SQLite
-- ============================================
-- 
-- Option A: Using SQLite CLI
-- Run this command in your terminal:
--   sqlite3 mahal.db "SELECT id, user_id, name, password, full_name, active, created_at, updated_at FROM admins;" > admins_export.txt
--
-- Option B: Using a SQLite GUI tool
-- Run this query:
--   SELECT id, user_id, name, password, full_name, active, created_at, updated_at FROM admins;
-- Copy the results
--
-- Option C: Generate INSERT statements directly from SQLite
-- Run this in SQLite:
--   .mode insert admins
--   .output admins_inserts.sql
--   SELECT id, user_id, name, password, full_name, active, created_at, updated_at FROM admins;
--   .quit
-- Then copy the generated INSERT statements here
--
-- ============================================
-- STEP 2: Replace Sample Data Below
-- ============================================
--
-- Replace the INSERT statements below with your actual admin data from SQLite.
-- Make sure to:
-- - Keep the password values as-is (they're BCrypt hashed, so safe to copy)
-- - Convert TEXT timestamps to TIMESTAMPTZ format if needed
-- - Set user_id = id::TEXT if user_id is NULL in SQLite (or use the actual user_id value)
--
-- ============================================
-- STEP 3: Insert Admins into Supabase
-- ============================================

-- First, clear existing admins if you want to start fresh (optional)
-- DELETE FROM admins;

-- Insert admin records
-- Replace these sample INSERT statements with your actual data from SQLite

-- Example format (replace with your actual data):
-- INSERT INTO admins (id, user_id, name, password, full_name, active, created_at, updated_at)
-- VALUES 
--     (1, '1', 'sam1@gmail.com', '$2a$10$...', 'Sam', 1, '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00'),
--     (2, '2', 'rajeesh@gmail.com', '$2a$10$...', 'Rajeesh', 1, '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00'),
--     (3, '3', 'revu@gmail.com', '$2a$10$...', 'Revu', 1, '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00')
-- ON CONFLICT (id) DO UPDATE SET
--     user_id = EXCLUDED.user_id,
--     name = EXCLUDED.name,
--     password = EXCLUDED.password,
--     full_name = EXCLUDED.full_name,
--     active = EXCLUDED.active,
--     updated_at = EXCLUDED.updated_at;

-- ============================================
-- Alternative: Use DEFAULT id (auto-increment)
-- ============================================
-- If you don't need to preserve the original IDs, you can omit the id column:
--
-- INSERT INTO admins (user_id, name, password, full_name, active, created_at, updated_at)
-- VALUES 
--     ('1', 'sam1@gmail.com', '$2a$10$...', 'Sam', 1, NOW(), NOW()),
--     ('2', 'rajeesh@gmail.com', '$2a$10$...', 'Rajeesh', 1, NOW(), NOW()),
--     ('3', 'revu@gmail.com', '$2a$10$...', 'Revu', 1, NOW(), NOW())
-- ON CONFLICT (user_id, name) DO UPDATE SET
--     password = EXCLUDED.password,
--     full_name = EXCLUDED.full_name,
--     active = EXCLUDED.active,
--     updated_at = NOW();

-- ============================================
-- STEP 4: Verify Data
-- ============================================

-- Check that all admins were inserted
SELECT id, user_id, name, full_name, active, created_at FROM admins ORDER BY id;

-- Verify user_id matches id (should be true for admins)
SELECT id, user_id, 
       CASE WHEN id::TEXT = user_id THEN 'OK' ELSE 'MISMATCH' END as user_id_check
FROM admins;

-- ============================================
-- DONE!
-- ============================================
-- Your admin data has been migrated to Supabase.
-- Admins can now be synced and used in the application.

