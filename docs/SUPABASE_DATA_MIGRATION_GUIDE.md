# Supabase Data Migration Guide - Assign Existing Data to Users

## Problem
After adding user_id filtering to all tables, existing records in Supabase (created before the update) don't have a user_id set, so they don't appear when syncing or when users log in.

## Solution
Assign all existing records in Supabase to a specific user (like "sam" or "rajeesh") so they become visible again.

## Recommended Approach: Use Supabase SQL Editor

The easiest and most reliable way to assign existing data in Supabase is to run SQL directly in the Supabase dashboard.

### Step 1: Open Supabase SQL Editor
1. Go to your Supabase project: https://supabase.com/dashboard/project/YOUR_PROJECT_ID
2. Click on "SQL Editor" in the left sidebar
3. Click "New Query"

### Step 2: Find the User ID
Run this query to see all admin users and their IDs:
```sql
SELECT id, name, full_name FROM admins ORDER BY id;
```

Note: 
- If your `user_id` column stores UUID, you'll see UUIDs like `123e4567-e89b-12d3-a456-426614174000`
- If your `user_id` column stores TEXT (admin ID), you'll see numeric IDs like `1`, `2`, `3`

### Step 3: Run the Migration Script

#### Option A: If user_id is TEXT (stores admin ID as text)

Use the script in `sql/assign_existing_data_to_user_supabase.sql`, but first run this to find the ID:

```sql
-- Find the user ID for "sam"
SELECT id::text as user_id_text, name, full_name FROM admins WHERE name = 'sam1@gmail.com' OR full_name = 'sam';
```

Then update the script, replacing `<USER_ID>` with the actual ID (e.g., `'1'` for user with id=1).

Or use a direct query that finds the ID automatically:

```sql
-- Assign all existing records to "sam" (user with full_name = 'sam')
UPDATE members SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE incomes SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE expenses SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE due_collections SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE masjids SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE committees SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE staff SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE staff_salaries SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE income_types SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE due_types SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE events SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE inventory_items SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE houses SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE damaged_items SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE rent_items SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE rents SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE marriage_certificates SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE death_certificates SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE jamath_certificates SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
UPDATE custom_certificates SET user_id = (SELECT id::text FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL OR user_id = '';
```

**To assign to a different user, replace `'sam'` with the desired admin name:**
- For "rajeesh": `WHERE full_name = 'rajeesh'`
- For "revu": `WHERE full_name = 'revu'`
- Or use email: `WHERE name = 'sam1@gmail.com'`

#### Option B: If user_id is UUID type

If your user_id column is UUID type, use:

```sql
-- Find UUID for user
SELECT id::uuid, name, full_name FROM admins WHERE full_name = 'sam';

-- Then use UUID format (replace with actual UUID):
UPDATE members SET user_id = '123e4567-e89b-12d3-a456-426614174000'::uuid WHERE user_id IS NULL;
-- ... repeat for other tables
```

Or convert automatically:

```sql
UPDATE members SET user_id = (SELECT id::uuid FROM admins WHERE full_name = 'sam' LIMIT 1) WHERE user_id IS NULL;
-- ... repeat for other tables
```

### Step 4: Verify Migration

After running the updates, verify the migration worked:

```sql
-- Check record counts by user_id
SELECT user_id, COUNT(*) FROM members GROUP BY user_id;
SELECT user_id, COUNT(*) FROM incomes GROUP BY user_id;
SELECT user_id, COUNT(*) FROM masjids GROUP BY user_id;

-- Check if any records still don't have user_id
SELECT COUNT(*) as missing_user_id FROM members WHERE user_id IS NULL OR user_id = '';
SELECT COUNT(*) as missing_user_id FROM incomes WHERE user_id IS NULL OR user_id = '';
```

If the counts show 0 for `missing_user_id`, the migration was successful!

## Important Notes

1. **Row Level Security (RLS)**: Make sure RLS policies are set up correctly in Supabase so users can only see their own data. See `sql/migrate_add_user_id_supabase.sql` for RLS policy examples.

2. **Backup First**: Before running bulk updates, consider backing up your Supabase database (Supabase Dashboard > Settings > Database > Backups).

3. **Test in Development**: If you have a staging/dev environment, test the migration there first.

4. **Sync After Migration**: After assigning user_ids in Supabase, your local sync should work correctly. All existing records will now have user_id and will be properly filtered.

## Troubleshooting

**Error: "column user_id does not exist"**
- Run `sql/migrate_add_user_id_supabase.sql` first to add the user_id column to all tables

**Error: "violates foreign key constraint"**
- Check that the user_id value exists in your admins/users table

**No records updated**
- Check if records already have user_id set: `SELECT user_id, COUNT(*) FROM members GROUP BY user_id;`
- Verify the WHERE condition matches existing records



