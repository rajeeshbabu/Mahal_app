-- Template for inserting users into Supabase admins table
-- REPLACE THE PLACEHOLDERS WITH YOUR ACTUAL USER DATA

-- Step 1: Make sure the admins table exists (run CREATE_ADMINS_TABLE_SUPABASE.sql first)

-- Step 2: Insert your users (REPLACE the values below)
-- Get the actual values from your local SQLite database:
-- sqlite3 frontend/mahal.db "SELECT id, name, password, full_name, active FROM admins ORDER BY id;"

INSERT INTO admins (name, password, full_name, active, created_at, updated_at)
VALUES 
    -- User 1: REPLACE these values
    ('user1@example.com', '$2a$10$REPLACE_WITH_ACTUAL_BCRYPT_HASH', 'User One Full Name', 1, NOW(), NOW()),
    
    -- User 2: REPLACE these values
    ('user2@example.com', '$2a$10$REPLACE_WITH_ACTUAL_BCRYPT_HASH', 'User Two Full Name', 1, NOW(), NOW())
    
ON CONFLICT (name) DO NOTHING;  -- Prevents errors if users already exist

-- Verification query
SELECT id, name, full_name, active, created_at FROM admins ORDER BY id;

