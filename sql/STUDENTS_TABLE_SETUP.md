# Students Table Setup for Supabase

## Quick Setup

The students table is missing in Supabase. To fix this, run the SQL script in your Supabase SQL Editor.

## Steps

1. **Open Supabase Dashboard**
   - Go to your Supabase project
   - Navigate to **SQL Editor**

2. **Run the SQL Script**
   - Copy the contents of `sql/CREATE_STUDENTS_TABLE_SUPABASE.sql`
   - Paste into the SQL Editor
   - Click **Run** or press `Ctrl+Enter`

3. **Verify**
   - Go to **Table Editor** in Supabase
   - You should now see the `students` table
   - Check that it has all columns including `user_id`

## What the Script Does

- Creates the `students` table with all required columns
- Adds `user_id` column for user isolation
- Creates index on `user_id` for performance
- Enables Row Level Security (RLS)
- Creates RLS policies to allow sync operations

## Table Structure

The students table includes:
- `id` (BIGSERIAL PRIMARY KEY)
- `user_id` (TEXT NOT NULL) - for user isolation
- `name` (TEXT NOT NULL)
- `course` (TEXT)
- `admission_number` (TEXT)
- `admission_date` (DATE)
- `mobile` (TEXT)
- `email` (TEXT)
- `address` (TEXT)
- `father_name` (TEXT)
- `mother_name` (TEXT)
- `guardian_mobile` (TEXT)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

## After Setup

Once the table is created:
1. The Java application will automatically sync student data to Supabase
2. New students added in the app will appear in Supabase
3. Existing local student data can be synced using the sync feature

## Troubleshooting

If you get an error that the table already exists:
- The script uses `CREATE TABLE IF NOT EXISTS`, so it's safe to run multiple times
- If you need to recreate the table, drop it first: `DROP TABLE IF EXISTS students;`

