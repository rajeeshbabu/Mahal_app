-- SQL Script: Fix students table columns in Supabase
-- Run this in Supabase SQL Editor to ensure all columns exist
-- This script is safe to run multiple times (uses IF NOT EXISTS)

-- ============================================
-- STEP 1: Create table if it doesn't exist
-- ============================================

CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    course TEXT,
    admission_number TEXT,
    admission_date DATE,
    mobile TEXT,
    email TEXT,
    address TEXT,
    father_name TEXT,
    mother_name TEXT,
    guardian_mobile TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- STEP 2: Add missing columns if table already exists
-- ============================================

-- Add user_id if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS user_id TEXT;

-- Add name if missing (should already exist, but safe to run)
ALTER TABLE students ADD COLUMN IF NOT EXISTS name TEXT NOT NULL;

-- Add course if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS course TEXT;

-- Add admission_number if missing (THIS IS THE MISSING COLUMN!)
ALTER TABLE students ADD COLUMN IF NOT EXISTS admission_number TEXT;

-- Add admission_date if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS admission_date DATE;

-- Add mobile if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS mobile TEXT;

-- Add email if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS email TEXT;

-- Add address if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS address TEXT;

-- Add father_name if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS father_name TEXT;

-- Add mother_name if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS mother_name TEXT;

-- Add guardian_mobile if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS guardian_mobile TEXT;

-- Add notes if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS notes TEXT;

-- Add timestamps if missing
ALTER TABLE students ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE students ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ============================================
-- STEP 3: Create/Update index on user_id
-- ============================================

CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);

-- ============================================
-- STEP 4: Enable Row Level Security (RLS)
-- ============================================

ALTER TABLE students ENABLE ROW LEVEL SECURITY;

-- ============================================
-- STEP 5: Create/Update RLS policies
-- ============================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Allow all operations for anon role on students" ON students;

-- Create policy to allow all operations (for service role key usage)
CREATE POLICY "Allow all operations for anon role on students" ON students
    FOR ALL USING (true) WITH CHECK (true);

-- ============================================
-- STEP 6: Refresh schema cache (PostgREST)
-- ============================================
-- Note: PostgREST caches the schema. After running this script,
-- you may need to wait a few seconds or restart PostgREST for changes to take effect.
-- In Supabase, the schema cache usually refreshes automatically within a few seconds.

-- ============================================
-- VERIFICATION QUERY
-- ============================================
-- Run this to verify all columns exist:
-- SELECT column_name, data_type 
-- FROM information_schema.columns 
-- WHERE table_name = 'students' 
-- ORDER BY ordinal_position;

-- ============================================
-- DONE!
-- ============================================
-- After running this script:
-- 1. Wait 5-10 seconds for PostgREST schema cache to refresh
-- 2. Try syncing again from your Java application
-- 3. The admission_number column should now be recognized

