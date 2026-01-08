# Complete Supabase Migration Steps

## Current Status
The `user_id` columns have **NOT** been added to Supabase yet. You need to run the migration in two steps.

## Step 1: Add user_id Columns

1. Open Supabase SQL Editor: https://supabase.com/dashboard/project/hkckhwxpxfylaeqnlrrv
2. Click **"SQL Editor"** → **"New Query"**
3. Open the file: `sql/ADD_USER_ID_COLUMNS_SUPABASE.sql`
4. Copy the entire contents
5. Paste into Supabase SQL Editor
6. Click **"Run"** (or press `Ctrl+Enter`)

**Expected Result:** You should see "Success" messages for each ALTER TABLE statement.

**Verify:** After running, run this query to confirm columns were added:
```sql
SELECT table_name, column_name 
FROM information_schema.columns 
WHERE column_name = 'user_id' 
AND table_schema = 'public'
ORDER BY table_name;
```

This should return 20 rows (one for each table).

## Step 2: Assign Existing Records to a User

After Step 1 succeeds, run the assignment script:

1. In the same SQL Editor, open: `sql/ASSIGN_DATA_TO_USER_SUPABASE.sql`
2. Copy the entire contents
3. Paste into Supabase SQL Editor
4. **IMPORTANT:** If you want to assign to a different user than sam:
   - Change all `'1'` to `'2'` for rajeesh
   - Change all `'1'` to `'3'` for revu
5. Click **"Run"**

**Expected Result:** You should see "Success" messages showing how many rows were updated for each table.

## Step 3: Verify Everything Worked

Run this verification query:
```sql
-- Check record counts by user_id
SELECT 'members' as table_name, user_id, COUNT(*) as count FROM members GROUP BY user_id
UNION ALL
SELECT 'incomes', user_id, COUNT(*) FROM incomes GROUP BY user_id
UNION ALL
SELECT 'masjids', user_id, COUNT(*) FROM masjids GROUP BY user_id
UNION ALL
SELECT 'committees', user_id, COUNT(*) FROM committees GROUP BY user_id
ORDER BY table_name, user_id;
```

This should show all records assigned to user_id = '1' (or whatever you chose).

## Troubleshooting

**If ALTER TABLE fails:**
- Check if tables exist: `SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;`
- Make sure you're in the correct database/schema

**If UPDATE fails:**
- Make sure Step 1 completed successfully first
- Check if there are any records: `SELECT COUNT(*) FROM members;`

**If verification shows no user_id columns:**
- Step 1 didn't run successfully - re-run it
- Check for error messages in the Supabase SQL Editor



