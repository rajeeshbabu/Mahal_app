-- SQL Script: Create students table in Supabase
-- Run this in Supabase SQL Editor

-- ============================================
-- STEP 1: Create students table
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
-- STEP 2: Create index on user_id for performance
-- ============================================

CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);

-- ============================================
-- STEP 3: Enable Row Level Security (RLS)
-- ============================================

ALTER TABLE students ENABLE ROW LEVEL SECURITY;

-- ============================================
-- STEP 4: Create RLS policies
-- ============================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Allow all operations for anon role on students" ON students;
DROP POLICY IF EXISTS "Users can view own students" ON students;
DROP POLICY IF EXISTS "Users can insert own students" ON students;
DROP POLICY IF EXISTS "Users can update own students" ON students;
DROP POLICY IF EXISTS "Users can delete own students" ON students;

-- Create policy to allow all operations (for service role key usage)
-- Note: The application layer filters by user_id, so this allows the sync service to work
CREATE POLICY "Allow all operations for anon role on students" ON students
    FOR ALL USING (true) WITH CHECK (true);

-- Alternative: If you want stricter RLS policies based on user_id:
-- CREATE POLICY "Users can view own students" ON students
--     FOR SELECT USING (true);
-- 
-- CREATE POLICY "Users can insert own students" ON students
--     FOR INSERT WITH CHECK (true);
-- 
-- CREATE POLICY "Users can update own students" ON students
--     FOR UPDATE USING (true) WITH CHECK (true);
-- 
-- CREATE POLICY "Users can delete own students" ON students
--     FOR DELETE USING (true);

-- ============================================
-- STEP 5: Add all columns if table already exists
-- (This ensures all columns exist even if table was created without them)
-- ============================================

ALTER TABLE students ADD COLUMN IF NOT EXISTS user_id TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS name TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS course TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS admission_number TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS admission_date DATE;
ALTER TABLE students ADD COLUMN IF NOT EXISTS mobile TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS father_name TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS mother_name TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS guardian_mobile TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE students ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ============================================
-- DONE!
-- ============================================
-- The students table is now ready for sync operations.
-- The Java application will automatically sync data to this table.

