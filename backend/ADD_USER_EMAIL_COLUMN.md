# Adding user_email Column to Subscriptions Table

## Overview

The `user_email` field has been added to the `Subscription` entity model. This guide explains how to add the column to both the local database and Supabase.

## Automatic Migration (Recommended)

If you're using **H2 database** with `spring.jpa.hibernate.ddl-auto=update` (default), Hibernate will **automatically add** the `user_email` column when you restart the backend. No manual SQL needed!

### Steps:

1. **Restart the backend application**
   - Hibernate will detect the new field in the `Subscription` entity
   - It will automatically add the `user_email` column to the `subscriptions` table
   - Existing records will have `user_email` set to `NULL`

2. **Update existing records** (if any exist):
   ```sql
   -- Run this in H2 Console (http://localhost:8080/h2-console)
   UPDATE subscriptions 
   SET user_email = user_id 
   WHERE user_email IS NULL;
   ```
   
   This copies the `user_id` value (which is the email) to `user_email` for existing records.

## Manual Migration (If Needed)

### For Local Database (H2/MySQL)

If automatic migration doesn't work, run the SQL script:

**H2 Database:**
```sql
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS user_email VARCHAR(255);
UPDATE subscriptions SET user_email = user_id WHERE user_email IS NULL;
```

**MySQL Database:**
```sql
ALTER TABLE subscriptions ADD COLUMN user_email VARCHAR(255);
UPDATE subscriptions SET user_email = user_id WHERE user_email IS NULL OR user_email = '';
```

Or use the provided script: `backend/sql/ADD_USER_EMAIL_LOCAL_DB.sql`

**How to run:**
- **H2**: Open H2 Console at `http://localhost:8080/h2-console` (URL: `jdbc:h2:file:./data/mahal_db`)
- **MySQL**: Use MySQL client or phpMyAdmin

### For Supabase

Run the SQL script in Supabase SQL Editor:

```sql
-- Add user_email column
ALTER TABLE subscriptions 
ADD COLUMN IF NOT EXISTS user_email TEXT;

-- Create index
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_email ON subscriptions(user_email);

-- Update existing records
UPDATE subscriptions 
SET user_email = user_id 
WHERE user_email IS NULL OR user_email = '';
```

Or use the provided script: `backend/sql/ADD_USER_EMAIL_SUPABASE.sql`

**How to run:**
1. Open Supabase Dashboard
2. Go to **SQL Editor**
3. Paste the SQL above (or contents of `ADD_USER_EMAIL_SUPABASE.sql`)
4. Click **Run**

## Verification

### Check Local Database

**H2 Console:**
```sql
SELECT id, user_id, user_email, status FROM subscriptions LIMIT 5;
```

**MySQL:**
```sql
SELECT id, user_id, user_email, status FROM subscriptions LIMIT 5;
```

### Check Supabase

**Supabase SQL Editor:**
```sql
SELECT id, user_id, user_email, status FROM subscriptions LIMIT 5;
```

Or check in **Table Editor** → `subscriptions` table.

## What Happens for New Subscriptions

After the column is added:
- When a new subscription is created, the `user_email` field will be automatically populated with the user's email
- The backend code now sets: `subscription.setUserEmail(userId)` where `userId` is the email from the frontend
- Both `user_id` and `user_email` will have the same value (the user's email)

## Troubleshooting

### Error: "Column user_email does not exist"

**Cause:** The column hasn't been added yet.

**Solution:**
1. For local DB: Restart backend (Hibernate auto-adds) OR run manual SQL
2. For Supabase: Run the migration SQL script

### Error: "user_email is NULL for existing records"

**Cause:** Old records were created before the column was added.

**Solution:** Run the UPDATE query:
```sql
UPDATE subscriptions SET user_email = user_id WHERE user_email IS NULL;
```

### Hibernate not adding column automatically

**Check:**
1. `spring.jpa.hibernate.ddl-auto=update` is set in `application.properties`
2. Backend was restarted after adding the field to the entity
3. Check backend logs for Hibernate DDL messages

**If still not working:** Run the manual SQL migration script.

