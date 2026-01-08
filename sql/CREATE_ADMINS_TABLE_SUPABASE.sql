-- SQL Script: Create admins table in Supabase
-- Run this in Supabase SQL Editor

-- ============================================
-- STEP 1: Create admins table
-- ============================================

CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT,
    active INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- STEP 2: Create index on user_id for user isolation and performance
-- ============================================

CREATE INDEX IF NOT EXISTS idx_admins_user_id ON admins(user_id);

-- ============================================
-- STEP 3: Create unique index on name (email) to prevent duplicates per user
-- ============================================

CREATE UNIQUE INDEX IF NOT EXISTS idx_admins_name_unique ON admins(user_id, name);

-- ============================================
-- STEP 4: Create index on active status for faster queries
-- ============================================

CREATE INDEX IF NOT EXISTS idx_admins_active ON admins(active);

-- ============================================
-- STEP 5: Enable Row Level Security (RLS)
-- ============================================

ALTER TABLE admins ENABLE ROW LEVEL SECURITY;

-- ============================================
-- STEP 6: Create RLS policies
-- ============================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Allow all operations for anon role on admins" ON admins;
DROP POLICY IF EXISTS "Users can view own admins" ON admins;
DROP POLICY IF EXISTS "Users can insert own admins" ON admins;
DROP POLICY IF EXISTS "Users can update own admins" ON admins;
DROP POLICY IF EXISTS "Users can delete own admins" ON admins;

-- Create policy to allow all operations (for service role key usage)
-- Note: The application layer handles authentication, so this allows the sync service to work
-- For production, you may want stricter policies
CREATE POLICY "Allow all operations for anon role on admins" ON admins
    FOR ALL USING (true) WITH CHECK (true);

-- ============================================
-- STEP 7: Add all columns if table already exists
-- (This ensures all columns exist even if table was created without them)
-- ============================================

ALTER TABLE admins ADD COLUMN IF NOT EXISTS user_id TEXT;
ALTER TABLE admins ADD COLUMN IF NOT EXISTS name TEXT;
ALTER TABLE admins ADD COLUMN IF NOT EXISTS password TEXT;
ALTER TABLE admins ADD COLUMN IF NOT EXISTS full_name TEXT;
ALTER TABLE admins ADD COLUMN IF NOT EXISTS active INTEGER DEFAULT 1;
ALTER TABLE admins ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE admins ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- If user_id was added to existing table, set a default value for existing rows
-- Update this to match your actual user IDs
UPDATE admins SET user_id = id::TEXT WHERE user_id IS NULL OR user_id = '';

-- ============================================
-- DONE!
-- ============================================
-- The admins table is now ready in Supabase.
-- Note: Password field stores BCrypt hashed passwords (one-way encryption).
-- The Java application can now sync admin data to this table.

