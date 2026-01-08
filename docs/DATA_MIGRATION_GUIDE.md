# Data Migration Guide - Assign Existing Data to Users

## Problem
After adding user_id filtering to all tables, existing records (created before the update) don't have a user_id set, so they don't appear when users log in.

## Solution
Assign all existing records to a specific user (like "sam" or "user1") so they become visible again.

## Quick Fix - Using Java Utility (Recommended)

### Step 1: Compile the migration utility
```bash
javac -cp "lib/sqlite-jdbc.jar;lib/org.json.jar;bin" -d bin src/com/mahal/util/MigrateExistingData.java
```

### Step 2: Run the migration utility
```bash
java -cp "lib/sqlite-jdbc.jar;bin" com.mahal.util.MigrateExistingData user1
```

Replace `user1` with the admin username you want to assign data to:
- For "sam": `java -cp "lib/sqlite-jdbc.jar;bin" com.mahal.util.MigrateExistingData sam`
- For "user1": `java -cp "lib/sqlite-jdbc.jar;bin" com.mahal.util.MigrateExistingData user1`

### What it does:
1. Lists all admin users in the database
2. Finds the user ID for the specified admin name
3. Updates all existing records (where user_id is NULL or empty) to assign them to that user
4. Makes all existing data visible to that user when they log in

## Alternative: Manual SQL Script

If you prefer SQL, you can use the SQL script `sql/assign_existing_data_to_user.sql`:

1. First, find the admin user ID:
```sql
SELECT id, name, full_name FROM admins;
```

2. Then, edit the SQL script and replace `<USER_ID>` with the actual ID (e.g., if "sam" has id=1, replace with 1)

3. Run the script in your SQLite database

## Which User Should I Assign To?

**If you want to keep existing data with "sam":**
- Run: `java -cp "lib/sqlite-jdbc.jar;bin" com.mahal.util.MigrateExistingData sam`

**If you want to assign existing data to "user1":**
- Run: `java -cp "lib/sqlite-jdbc.jar;bin" com.mahal.util.MigrateExistingData user1`

**Note:** All existing records will be assigned to ONE user. If you have multiple users with existing data, you may need to manually separate them later.

## Verification

After running the migration, log in as the user you assigned data to, and you should see all the existing records.

You can also verify in SQLite:
```sql
SELECT user_id, COUNT(*) FROM members GROUP BY user_id;
SELECT user_id, COUNT(*) FROM incomes GROUP BY user_id;
```



