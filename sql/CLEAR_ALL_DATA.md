# How to Clear All Data from Supabase and SQLite

## Option 1: Clear All Data (Recommended)

### Step 1: Clear SQLite Database

#### Method A: Delete the database file (removes everything)
1. Close your application if it's running
2. Find your SQLite database file (usually `mahal.db` in your project root or application directory)
3. Delete the file: `mahal.db`
4. The database will be recreated automatically when you restart the application (tables will be empty)

#### Method B: Delete all records using SQL (keeps table structure)
1. Open a SQLite browser tool (like DB Browser for SQLite) or use command line
2. Run the single SQL script from `sql/CLEAR_ALL_DATA_SQLITE.sql`:

```sql
BEGIN TRANSACTION;

DELETE FROM sync_queue;
DELETE FROM sync_metadata;
DELETE FROM jamath_certificates;
DELETE FROM custom_certificates;
DELETE FROM students;
DELETE FROM houses;
DELETE FROM committees;
DELETE FROM staff_salaries;
DELETE FROM staff;
DELETE FROM rents;
DELETE FROM rent_items;
DELETE FROM damaged_items;
DELETE FROM inventory_items;
DELETE FROM events;
DELETE FROM masjids;
DELETE FROM due_collections;
DELETE FROM expenses;
DELETE FROM incomes;
DELETE FROM members;
DELETE FROM admins;

COMMIT;
```

**Or simply run:** `sqlite3 mahal.db < sql/CLEAR_ALL_DATA_SQLITE.sql`

**Note:** The `sync_queue` and `sync_metadata` tables store sync operations and metadata, so clearing them will also clear sync history.

### Step 2: Clear Supabase Database

1. Go to your Supabase Dashboard: https://supabase.com/dashboard
2. Select your project
3. Click on **"SQL Editor"** in the left sidebar
4. Click **"New Query"**
5. Copy and paste the contents from `sql/CLEAR_ALL_DATA_SUPABASE.sql`:

```sql
BEGIN;

DELETE FROM sync_queue;
DELETE FROM sync_metadata;
DELETE FROM jamath_certificates;
DELETE FROM custom_certificates;
DELETE FROM students;
DELETE FROM houses;
DELETE FROM committees;
DELETE FROM staff_salaries;
DELETE FROM staff;
DELETE FROM rents;
DELETE FROM rent_items;
DELETE FROM damaged_items;
DELETE FROM inventory_items;
DELETE FROM events;
DELETE FROM masjids;
DELETE FROM due_collections;
DELETE FROM expenses;
DELETE FROM incomes;
DELETE FROM members;
DELETE FROM admins;

COMMIT;
```

6. Click **"Run"** to execute the script (all deletions happen in a single transaction)
7. All data will be deleted from Supabase

---

## Option 2: Delete Tables Completely (Nuclear Option)

**⚠️ WARNING: This will delete both data AND table structures. You'll need to recreate the tables afterwards.**

### Step 1: Delete SQLite Tables

```sql
-- Drop all tables (in reverse order of dependencies)
DROP TABLE IF EXISTS sync_queue;
DROP TABLE IF EXISTS sync_metadata;
DROP TABLE IF EXISTS jamath_certificates;
DROP TABLE IF EXISTS custom_certificates;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS houses;
DROP TABLE IF EXISTS committees;
DROP TABLE IF EXISTS staff_salaries;
DROP TABLE IF EXISTS staff;
DROP TABLE IF EXISTS rents;
DROP TABLE IF EXISTS rent_items;
DROP TABLE IF EXISTS damaged_items;
DROP TABLE IF EXISTS inventory_items;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS masjids;
DROP TABLE IF EXISTS due_collections;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS incomes;
DROP TABLE IF EXISTS members;
DROP TABLE IF EXISTS admins;
```

**After dropping tables, restart your application to recreate them automatically.**

### Step 2: Delete Supabase Tables

1. Go to Supabase Dashboard → SQL Editor
2. Run this script:

```sql
-- Drop all tables (in reverse order of dependencies)
DROP TABLE IF EXISTS sync_queue CASCADE;
DROP TABLE IF EXISTS sync_metadata CASCADE;
DROP TABLE IF EXISTS jamath_certificates CASCADE;
DROP TABLE IF EXISTS custom_certificates CASCADE;
DROP TABLE IF EXISTS students CASCADE;
DROP TABLE IF EXISTS houses CASCADE;
DROP TABLE IF EXISTS committees CASCADE;
DROP TABLE IF EXISTS staff_salaries CASCADE;
DROP TABLE IF EXISTS staff CASCADE;
DROP TABLE IF EXISTS rents CASCADE;
DROP TABLE IF EXISTS rent_items CASCADE;
DROP TABLE IF EXISTS damaged_items CASCADE;
DROP TABLE IF EXISTS inventory_items CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS masjids CASCADE;
DROP TABLE IF EXISTS due_collections CASCADE;
DROP TABLE IF EXISTS expenses CASCADE;
DROP TABLE IF EXISTS incomes CASCADE;
DROP TABLE IF EXISTS members CASCADE;
DROP TABLE IF EXISTS admins CASCADE;
```

3. After dropping, recreate the tables by running the schema script:
   - Run `supabase_schema.sql` in the Supabase SQL Editor

---

## Option 3: Clear Only Specific Tables

If you only want to clear specific tables (e.g., only admins):

### SQLite:
```sql
DELETE FROM admins;
```

### Supabase:
```sql
DELETE FROM admins;
```

---

## Quick Reference Commands

### SQLite Command Line:
```bash
# Open SQLite database
sqlite3 mahal.db

# Run delete commands
sqlite3 mahal.db "DELETE FROM admins; DELETE FROM members; DELETE FROM incomes; ..."

# Or use a SQL file
sqlite3 mahal.db < clear_all_data.sql
```

### Supabase:
- Use the SQL Editor in the Supabase Dashboard
- Or use the Supabase CLI if you have it installed

---

## Important Notes

1. **Backup First**: Before clearing data, make a backup if you might need it later
2. **Sync Queue**: Clearing `sync_queue` will also clear pending sync operations
3. **Foreign Keys**: Some tables may have foreign key constraints - delete in the correct order
4. **Recreate Admins**: After clearing, you'll need to register new admin users
5. **Sync Metadata**: Clearing `sync_metadata` will reset sync timestamps

