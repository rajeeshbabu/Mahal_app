# Migrate Admins from SQLite to Supabase

This guide explains how to copy admin data from your local SQLite database to Supabase.

## Prerequisites

1. ✅ Admins table exists in Supabase (run `CREATE_ADMINS_TABLE_SUPABASE.sql` first)
2. ✅ You have access to your SQLite database file (`mahal.db`)
3. ✅ You have access to Supabase SQL Editor

## Method 1: Manual Export/Import (Recommended)

### Step 1: Export Data from SQLite

Open your SQLite database and run this query to see your admin data:

```sql
SELECT id, user_id, name, password, full_name, active, created_at, updated_at 
FROM admins 
ORDER BY id;
```

**Example output:**
```
id | user_id | name              | password          | full_name | active | created_at           | updated_at
1  | 1       | sam1@gmail.com    | $2a$10$abc...     | Sam       | 1      | 2024-01-01 00:00:00  | 2024-01-01 00:00:00
2  | 2       | rajeesh@gmail.com | $2a$10$def...     | Rajeesh   | 1      | 2024-01-01 00:00:00  | 2024-01-01 00:00:00
```

### Step 2: Create INSERT Statements

Open Supabase SQL Editor and create INSERT statements like this:

```sql
-- Insert admins into Supabase
INSERT INTO admins (id, user_id, name, password, full_name, active, created_at, updated_at)
VALUES 
    (1, '1', 'sam1@gmail.com', '$2a$10$abc...', 'Sam', 1, '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00'),
    (2, '2', 'rajeesh@gmail.com', '$2a$10$def...', 'Rajeesh', 1, '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00')
ON CONFLICT (user_id, name) DO UPDATE SET
    password = EXCLUDED.password,
    full_name = EXCLUDED.full_name,
    active = EXCLUDED.active,
    updated_at = EXCLUDED.updated_at;
```

**Important Notes:**
- Copy the password values exactly as they appear (they're BCrypt hashed, so safe)
- Convert timestamps to PostgreSQL format: `'2024-01-01 00:00:00+00'` (add timezone `+00`)
- If `user_id` is NULL in SQLite, use `id::TEXT` as the user_id value
- The `ON CONFLICT` clause will update existing records instead of failing

### Step 3: Run in Supabase

1. Paste the INSERT statements into Supabase SQL Editor
2. Click **Run** or press `Ctrl+Enter`
3. Verify the data:

```sql
SELECT id, user_id, name, full_name, active FROM admins ORDER BY id;
```

## Method 2: Using SQLite CLI (Advanced)

### Step 1: Generate INSERT Statements

Open terminal/command prompt and navigate to your project directory:

```bash
# For Windows (PowerShell)
sqlite3 mahal.db

# For Linux/Mac
sqlite3 mahal.db
```

Then run:

```sql
.mode insert admins
.output admins_inserts.sql
SELECT id, COALESCE(user_id, CAST(id AS TEXT)) as user_id, name, password, full_name, active, created_at, updated_at FROM admins;
.quit
```

This creates a file `admins_inserts.sql` with INSERT statements.

### Step 2: Convert Format

Open `admins_inserts.sql` and convert from SQLite format to PostgreSQL format:

**SQLite format:**
```sql
INSERT INTO admins VALUES(1,'1','sam1@gmail.com','$2a$10$...','Sam',1,'2024-01-01 00:00:00','2024-01-01 00:00:00');
```

**PostgreSQL format (Supabase):**
```sql
INSERT INTO admins (id, user_id, name, password, full_name, active, created_at, updated_at)
VALUES (1, '1', 'sam1@gmail.com', '$2a$10$...', 'Sam', 1, '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00');
```

Changes needed:
1. Add column names in parentheses
2. Add timezone to timestamps: `+00`
3. Add `ON CONFLICT` clause for safety

### Step 3: Run in Supabase

Copy the converted statements to Supabase SQL Editor and run them.

## Method 3: Use the Application Sync (Automatic)

If your application has sync functionality enabled:

1. Ensure admins are in the local SQLite database with `user_id` set
2. The sync service should automatically sync admins to Supabase
3. Check Supabase to verify the data was synced

## Verification

After migration, verify the data in Supabase:

```sql
-- Check all admins
SELECT id, user_id, name, full_name, active, created_at FROM admins ORDER BY id;

-- Verify user_id matches id (should be true)
SELECT id, user_id, 
       CASE WHEN id::TEXT = user_id THEN 'OK' ELSE 'MISMATCH' END as check_status
FROM admins;

-- Check for duplicates
SELECT name, COUNT(*) as count 
FROM admins 
GROUP BY name 
HAVING COUNT(*) > 1;
```

## Troubleshooting

### Error: "duplicate key value violates unique constraint"

**Solution:** The admin already exists. Use `ON CONFLICT` clause or delete existing records first:

```sql
-- Option 1: Use ON CONFLICT (recommended)
INSERT INTO admins (...) VALUES (...)
ON CONFLICT (user_id, name) DO UPDATE SET ...;

-- Option 2: Delete and reinsert (if you want to replace)
DELETE FROM admins WHERE id = 1;
INSERT INTO admins (...) VALUES (...);
```

### Error: "invalid input syntax for type timestamp"

**Solution:** Convert timestamps to PostgreSQL format. SQLite uses `'2024-01-01 00:00:00'`, but PostgreSQL needs timezone: `'2024-01-01 00:00:00+00'`

### Error: "column 'user_id' does not exist"

**Solution:** Make sure you ran `CREATE_ADMINS_TABLE_SUPABASE.sql` first to create the table with the user_id column.

### Passwords don't work after migration

**Solution:** Make sure you copied the password values exactly. BCrypt hashes are case-sensitive and must be copied character-by-character. Even a single character difference will make passwords fail.

## Security Notes

✅ **Safe to copy:** BCrypt hashed passwords (they're one-way encrypted)
✅ **Safe to copy:** All other admin data (names, emails, etc.)
⚠️ **Important:** Never share the SQLite database file or the generated INSERT statements publicly

## Next Steps

After migration:
1. Test login with migrated admin accounts
2. Verify all admins can access the system
3. Enable sync in the application if not already enabled
4. Future admin changes will sync automatically

