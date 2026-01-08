# Adding Registered Users to Both Databases

## Overview

This guide explains how to add your two registered users to:
1. **Backend H2 Database** (for subscriptions/backend services)
2. **Supabase Database** (for cloud sync)

## Step 1: Get User Information from Local Database

First, you need to get the user details from your local SQLite database (`frontend/mahal.db`).

### Option A: Using H2 Console (if backend has access)
Not applicable - backend doesn't have admins table.

### Option B: Using SQLite Browser or Command Line

Open your SQLite database and run:
```sql
SELECT id, name, password, full_name, active, created_at FROM admins ORDER BY id;
```

**Note:** The `password` field contains BCrypt hashed passwords. You'll need these exact hashes.

## Step 2: Add Users to Backend H2 Database

The backend H2 database is primarily for subscriptions. If you need an `admins` table there, create it:

```sql
-- Run in H2 Console: http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:file:./data/mahal_db

CREATE TABLE IF NOT EXISTS admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    active INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert your users (replace with actual values)
INSERT INTO admins (id, name, password, full_name, active, created_at, updated_at)
VALUES 
    (1, 'user1@example.com', '$2a$10$...', 'User One Full Name', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'user2@example.com', '$2a$10$...', 'User Two Full Name', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

**Important:** Copy the exact password hash from your SQLite database.

## Step 3: Add Users to Supabase

Run the provided script to create the admins table in Supabase:

1. **Create the table:** Run `backend/sql/CREATE_ADMINS_TABLE_SUPABASE.sql` in Supabase SQL Editor

2. **Insert your users:** Run this SQL (replace with your actual user data):

```sql
-- Insert users into Supabase admins table
-- Replace with your actual user data from the local database

INSERT INTO admins (name, password, full_name, active, created_at, updated_at)
VALUES 
    ('user1@example.com', '$2a$10$...', 'User One Full Name', 1, NOW(), NOW()),
    ('user2@example.com', '$2a$10$...', 'User Two Full Name', 1, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;  -- Prevents duplicate insertions
```

## Quick SQL Script Generator

If you have the user data, you can use this template:

```sql
-- For Supabase
INSERT INTO admins (name, password, full_name, active, created_at, updated_at)
VALUES 
    ('EMAIL_1', 'PASSWORD_HASH_1', 'FULL_NAME_1', 1, NOW(), NOW()),
    ('EMAIL_2', 'PASSWORD_HASH_2', 'FULL_NAME_2', 1, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- For Backend H2 (if needed)
INSERT INTO admins (id, name, password, full_name, active, created_at, updated_at)
VALUES 
    (1, 'EMAIL_1', 'PASSWORD_HASH_1', 'FULL_NAME_1', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'EMAIL_2', 'PASSWORD_HASH_2', 'FULL_NAME_2', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

## Important Notes

1. **Password Hashes:** 
   - BCrypt hashes start with `$2a$` or `$2b$`
   - Copy the EXACT hash from your local database - do NOT try to re-hash the password
   - BCrypt hashes include the salt and cannot be regenerated with the same salt

2. **User IDs:**
   - In SQLite: IDs are typically 1, 2, 3, etc.
   - In Supabase: Use BIGSERIAL (auto-increment) or match the SQLite IDs
   - In H2: Use BIGINT with AUTO_INCREMENT

3. **Email as Username:**
   - The `name` column stores the email address
   - This is what users use to log in

## Verification

After adding users:

**Supabase:**
```sql
SELECT id, name, full_name, active, created_at FROM admins ORDER BY id;
```

**Backend H2:**
```sql
SELECT id, name, full_name, active, created_at FROM admins ORDER BY id;
```

## Alternative: Use Supabase Auth

If you want to use Supabase's built-in authentication instead of a custom `admins` table:

1. Go to Supabase Dashboard → Authentication → Users
2. Click "Add User"
3. Enter email and password
4. Users will be stored in `auth.users` table

This is more secure and provides additional features like email verification, password reset, etc.

