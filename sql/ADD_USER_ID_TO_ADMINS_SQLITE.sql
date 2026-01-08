-- SQL Script: Add user_id column to admins table in SQLite
-- Run this script in your SQLite database to add user_id column to existing admins table
--
-- Usage:
--   sqlite3 mahal.db < ADD_USER_ID_TO_ADMINS_SQLITE.sql
--   OR run these statements in SQLite CLI or GUI tool

-- ============================================
-- STEP 1: Add user_id column if it doesn't exist
-- ============================================

-- SQLite doesn't support "ADD COLUMN IF NOT EXISTS", so we'll use a workaround
-- First, check if column exists, then add it if needed

-- Note: If you get "duplicate column name" error, the column already exists and you can skip this step

ALTER TABLE admins ADD COLUMN user_id TEXT;

-- ============================================
-- STEP 2: Set user_id = id for existing records
-- ============================================

-- For existing admins, set user_id to match the id (admin id = user_id)
UPDATE admins SET user_id = CAST(id AS TEXT) WHERE user_id IS NULL OR user_id = '';

-- ============================================
-- STEP 3: Make user_id NOT NULL (optional, if you want to enforce it)
-- ============================================

-- SQLite doesn't support ALTER COLUMN to add NOT NULL constraint directly
-- If you want NOT NULL constraint, you would need to:
-- 1. Create a new table with NOT NULL constraint
-- 2. Copy data
-- 3. Drop old table
-- 4. Rename new table
-- 
-- For now, we'll just ensure all records have user_id set (done in STEP 2)

-- ============================================
-- STEP 4: Create index on user_id for performance
-- ============================================

CREATE INDEX IF NOT EXISTS idx_admins_user_id ON admins(user_id);

-- ============================================
-- STEP 5: Create unique index on (user_id, name) to prevent duplicates per user
-- ============================================

CREATE UNIQUE INDEX IF NOT EXISTS idx_admins_user_name_unique ON admins(user_id, name);

-- ============================================
-- STEP 6: Verify the changes
-- ============================================

-- Check that user_id column exists and has values
SELECT id, user_id, name, full_name, active FROM admins ORDER BY id;

-- Verify user_id matches id for all records
SELECT id, user_id, 
       CASE WHEN CAST(id AS TEXT) = user_id THEN 'OK' ELSE 'MISMATCH' END as check_status
FROM admins;

-- ============================================
-- DONE!
-- ============================================
-- The admins table now has user_id column.
-- All existing admins have user_id set to their id value.
-- Future admin creation will set user_id automatically (via AdminDAO).

