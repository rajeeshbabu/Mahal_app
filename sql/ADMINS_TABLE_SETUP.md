# Admins Table Setup for Supabase

## Quick Setup

The admins table is now available in Supabase. To create it, run the SQL script in your Supabase SQL Editor.

## Steps

1. **Open Supabase Dashboard**
   - Go to your Supabase project
   - Navigate to **SQL Editor**

2. **Run the SQL Script**
   - Copy the contents of `sql/CREATE_ADMINS_TABLE_SUPABASE.sql`
   - Paste into the SQL Editor
   - Click **Run** or press `Ctrl+Enter`

   **OR** if you want to create all tables at once:
   - Use `supabase_schema.sql` which now includes the admins table

3. **Verify**
   - Go to **Table Editor** in Supabase
   - You should now see the `admins` table
   - Check that it has all required columns

## Table Structure

The admins table includes:
- `id` (BIGSERIAL PRIMARY KEY)
- `user_id` (TEXT NOT NULL) - for user isolation (same as other tables)
- `name` (TEXT NOT NULL) - stores email/username, unique per user_id
- `password` (TEXT NOT NULL) - stores BCrypt hashed password
- `full_name` (TEXT) - full name of the admin
- `active` (INTEGER DEFAULT 1) - 1 for active, 0 for inactive
- `created_at` (TIMESTAMPTZ) - creation timestamp
- `updated_at` (TIMESTAMPTZ) - last update timestamp

## Security Notes

⚠️ **Important Security Considerations:**

1. **Password Storage**: The `password` field stores BCrypt hashed passwords (one-way encryption). This is safe to sync to Supabase as the original passwords cannot be recovered from the hash.

2. **Row Level Security (RLS)**: The table has RLS enabled with a permissive policy. For production, you may want to restrict access:
   - Only allow authenticated users to read admins
   - Restrict write operations to specific roles
   - Consider using Supabase Auth instead of custom admin table for better security

3. **Unique Constraint**: The `name` column has a unique index combined with `user_id` to prevent duplicate email addresses per user.

4. **User Isolation**: The `user_id` column ensures admins are isolated per user, consistent with other tables in the system.

## After Setup

Once the table is created:
1. The Java application can sync admin data to Supabase
2. New admins added in the app can be synced to Supabase
3. Existing local admin data can be synced using the sync feature

## Troubleshooting

If you get an error that the table already exists:
- The script uses `CREATE TABLE IF NOT EXISTS`, so it's safe to run multiple times
- If you need to recreate the table, drop it first: `DROP TABLE IF EXISTS admins CASCADE;`

If you need to add sync functionality:
- Check if `AdminDAO` needs sync methods added
- The sync service should handle BCrypt hashed passwords correctly (they're just strings)

