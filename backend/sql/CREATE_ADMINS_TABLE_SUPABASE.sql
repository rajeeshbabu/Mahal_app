-- Create admins table in Supabase (to store user accounts)
-- This matches the structure of the local SQLite admins table
-- Run this in Supabase SQL Editor

CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,  -- Email address
    password TEXT NOT NULL,     -- BCrypt hashed password
    full_name TEXT,
    active INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create index on name (email) for faster lookups
CREATE INDEX IF NOT EXISTS idx_admins_name ON admins(name);

-- Create index on active status
CREATE INDEX IF NOT EXISTS idx_admins_active ON admins(active);

-- Enable Row Level Security (RLS)
ALTER TABLE admins ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Allow all operations (adjust based on your security needs)
-- For authentication, you may want to allow SELECT for login checks
CREATE POLICY "Allow admin operations"
    ON admins FOR ALL
    USING (true) WITH CHECK (true);

-- Add comment
COMMENT ON TABLE admins IS 'Admin user accounts for authentication';

