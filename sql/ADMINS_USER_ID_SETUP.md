# Add user_id Column to Admins Table in SQLite

This guide explains how to add the `user_id` column to the existing admins table in your SQLite database.

## Automatic (Recommended)

The `AdminDAO` already includes code to automatically add the `user_id` column when the application starts. Just run your application, and it will:

1. Check if the `user_id` column exists
2. Add it if missing
3. Create indexes

**However**, this won't set `user_id` values for existing admin records. You'll need to run the migration script below to set `user_id = id` for existing admins.

## Manual Migration (If Needed)

If you want to manually add the column, or if you have existing admin records that need `user_id` values:

### Step 1: Run the Migration Script

Open your SQLite database and run:

```sql
-- Add user_id column
ALTER TABLE admins ADD COLUMN user_id TEXT;

-- Set user_id = id for existing records
UPDATE admins SET user_id = CAST(id AS TEXT) WHERE user_id IS NULL OR user_id = '';

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_admins_user_id ON admins(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_admins_user_name_unique ON admins(user_id, name);
```

**OR** run the script file:

```bash
# Windows (PowerShell)
sqlite3 mahal.db < sql/ADD_USER_ID_TO_ADMINS_SQLITE.sql

# Linux/Mac
sqlite3 mahal.db < sql/ADD_USER_ID_TO_ADMINS_SQLITE.sql
```

### Step 2: Verify

Check that the column was added and values were set:

```sql
SELECT id, user_id, name, full_name FROM admins ORDER BY id;
```

You should see that `user_id` matches `id` for all admins.

## What Happens Next?

After running the migration:

1. ✅ Existing admins have `user_id` set to their `id` value
2. ✅ Future admin creation (via `AdminDAO.create()`) will automatically set `user_id = id`
3. ✅ The column is ready for syncing to Supabase

## Troubleshooting

### Error: "duplicate column name: user_id"

**Solution:** The column already exists. You can skip the `ALTER TABLE` step and just run the `UPDATE` statement to set values.

### Error: "no such column: user_id" after running ALTER TABLE

**Solution:** SQLite might need the table to be recreated. Try:
1. Backup your data first
2. Drop the table and recreate it using `AdminDAO` (the code will create it with `user_id`)
3. Re-insert your admin data

### Existing admins don't have user_id values

**Solution:** Run the UPDATE statement:
```sql
UPDATE admins SET user_id = CAST(id AS TEXT) WHERE user_id IS NULL OR user_id = '';
```

