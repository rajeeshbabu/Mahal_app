# Quick Guide: Migrate Admins from SQLite to Supabase

## Quick Method (Recommended)

### Step 1: Get Admin Data from SQLite

Open your SQLite database and run this query:

```sql
SELECT id, COALESCE(user_id, CAST(id AS TEXT)) as user_id, name, password, full_name, active, 
       created_at, updated_at 
FROM admins;
```

**Copy all the data** (especially the password column - it's BCrypt hashed).

### Step 2: Create INSERT Statement for Supabase

Go to Supabase SQL Editor and create an INSERT statement. For example, if you have 3 admins:

```sql
INSERT INTO admins (id, user_id, name, password, full_name, active, created_at, updated_at)
VALUES 
    (1, '1', 'sam1@gmail.com', '$2a$10$YOUR_HASHED_PASSWORD_HERE', 'Sam', 1, 
     '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00'),
    (2, '2', 'rajeesh@gmail.com', '$2a$10$YOUR_HASHED_PASSWORD_HERE', 'Rajeesh', 1,
     '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00'),
    (3, '3', 'revu@gmail.com', '$2a$10$YOUR_HASHED_PASSWORD_HERE', 'Revu', 1,
     '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00')
ON CONFLICT (user_id, name) DO UPDATE SET
    password = EXCLUDED.password,
    full_name = EXCLUDED.full_name,
    active = EXCLUDED.active,
    updated_at = NOW();
```

**Important:**
- Copy the password values EXACTLY as they appear (BCrypt hashes are case-sensitive)
- For timestamps, add `+00` at the end (e.g., `'2024-01-01 00:00:00+00'`)
- If you don't need to preserve IDs, you can omit the `id` column and let Supabase auto-generate

### Step 3: Run and Verify

Run the INSERT statement in Supabase, then verify:

```sql
SELECT id, user_id, name, full_name, active FROM admins ORDER BY id;
```

Done! ✅

## Alternative: Using SQLite Export

If you prefer, you can export to CSV and convert:

```bash
# Export to CSV
sqlite3 -header -csv mahal.db "SELECT id, COALESCE(user_id, CAST(id AS TEXT)) as user_id, name, password, full_name, active, created_at, updated_at FROM admins;" > admins.csv
```

Then manually convert the CSV data to INSERT statements.

## Notes

- ✅ Passwords (BCrypt hashed) are safe to copy - they're one-way encrypted
- ✅ The `ON CONFLICT` clause will update existing records instead of failing
- ⚠️ Make sure the admins table exists in Supabase first (run `CREATE_ADMINS_TABLE_SUPABASE.sql`)

