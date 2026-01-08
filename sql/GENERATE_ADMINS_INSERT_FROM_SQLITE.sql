-- SQLite Script: Generate INSERT statements for Supabase migration
-- 
-- Run this script in SQLite to generate INSERT statements that you can copy to Supabase
--
-- Usage:
--   1. Open SQLite database: sqlite3 mahal.db
--   2. Copy this script and paste it
--   3. Copy the output INSERT statements
--   4. Paste them into Supabase SQL Editor and run
--
-- ============================================

.mode insert admins
.output admins_supabase_inserts.sql

-- Generate INSERT statements for Supabase
SELECT 
    id,
    COALESCE(user_id, CAST(id AS TEXT)) as user_id,  -- Use id as user_id if user_id is NULL
    name,
    password,
    full_name,
    active,
    created_at,
    updated_at
FROM admins;

.quit

-- ============================================
-- After running, the file "admins_supabase_inserts.sql" will contain:
-- INSERT INTO admins VALUES(...), (...), ...;
--
-- You'll need to:
-- 1. Open admins_supabase_inserts.sql
-- 2. Convert to proper format with column names
-- 3. Adjust timestamps if needed (SQLite TEXT -> PostgreSQL TIMESTAMPTZ)
-- 4. Run in Supabase SQL Editor
-- ============================================

