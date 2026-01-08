# Apply Today's Changes to Supabase

## Summary of Changes Made Today

1. **Added `user_id` column** to all tables in local SQLite database
2. **Updated all DAOs** to filter queries by `user_id` for user isolation
3. **Updated sync services** to include `user_id` in all operations
4. **Assigned existing local data** to users using migration utility

## What Needs to be Done in Supabase

You need to apply the same changes to Supabase:

1. ✅ **Add `user_id` column** to all Supabase tables
2. ✅ **Assign existing Supabase records** to a user (e.g., "sam")
3. ⚠️ **Enable Row Level Security (RLS)** - Optional but recommended for extra security

## Quick Solution: Run Complete Migration Script

### Step 1: Open Supabase SQL Editor

1. Go to: https://supabase.com/dashboard/project/hkckhwxpxfylaeqnlrrv
2. Click **"SQL Editor"** in the left sidebar
3. Click **"New Query"**

### Step 2: Run the Complete Migration Script

1. Open the file: `sql/COMPLETE_SUPABASE_MIGRATION.sql`
2. Copy the entire contents
3. Paste into Supabase SQL Editor
4. **IMPORTANT**: Review the script - if you want to assign to a different user than "sam", change `full_name = 'sam'` to your desired user (e.g., `full_name = 'rajeesh'`)
5. Click **"Run"** or press `Ctrl+Enter`

### What the Script Does:

**Part 1: Adds user_id columns**
- Adds `user_id TEXT` column to all 20 tables
- Creates indexes on `user_id` for better query performance

**Part 2: Assigns existing data**
- Finds "sam" by `full_name`
- Updates all existing records (where user_id is NULL) to assign them to sam
- Covers all tables: members, incomes, expenses, masjids, committees, staff, events, inventory, certificates, etc.

**Part 3: Verification**
- Shows admin users
- Shows record counts by user_id
- Shows if any records still don't have user_id

## Alternative: Run Steps Separately

If you prefer to run steps separately:

### Step 1: Add user_id columns only
Run: `sql/migrate_add_user_id_supabase.sql`

### Step 2: Assign existing data
Run: `sql/assign_to_sam_supabase.sql` (or customize it)

## Verification

After running the script, check:

1. **All tables have user_id column:**
```sql
SELECT table_name, column_name 
FROM information_schema.columns 
WHERE column_name = 'user_id' 
ORDER BY table_name;
```

2. **All records are assigned:**
```sql
SELECT 'members' as table_name, COUNT(*) as total, 
       COUNT(CASE WHEN user_id IS NULL OR user_id = '' THEN 1 END) as missing_user_id
FROM members
UNION ALL
SELECT 'incomes', COUNT(*), COUNT(CASE WHEN user_id IS NULL OR user_id = '' THEN 1 END) FROM incomes
UNION ALL
SELECT 'masjids', COUNT(*), COUNT(CASE WHEN user_id IS NULL OR user_id = '' THEN 1 END) FROM masjids;
```

3. **Records are assigned to correct user:**
```sql
SELECT user_id, COUNT(*) FROM members GROUP BY user_id;
```

## Important Notes

1. **Backup First**: Consider backing up your Supabase database before running bulk updates
   - Supabase Dashboard > Settings > Database > Backups

2. **Change User if Needed**: 
   - To assign to "rajeesh": Change `full_name = 'sam'` to `full_name = 'rajeesh'`
   - To assign to "revu": Change `full_name = 'sam'` to `full_name = 'revu'`
   - To assign to email: Use `name = 'sam1@gmail.com'`

3. **RLS Policies**: The script includes commented-out RLS policy code. Uncomment if you want database-level security (optional - application already filters by user_id)

4. **After Migration**: 
   - All existing Supabase records will have user_id assigned
   - Sync operations will now correctly filter by user_id
   - Users will only see their own data when syncing

## Troubleshooting

**Error: "column user_id does not exist"**
- Part 1 should have created it. Check if the ALTER TABLE statements ran successfully.

**Error: "relation admins does not exist"**
- Check your admin table name - it might be `users` or `admin_users` instead of `admins`

**No records updated**
- Check if records already have user_id: `SELECT user_id, COUNT(*) FROM members GROUP BY user_id;`
- Check if "sam" exists: `SELECT id, name, full_name FROM admins WHERE full_name = 'sam';`

**Records assigned to wrong user**
- You can reassign by running the UPDATE statements again with a different user



