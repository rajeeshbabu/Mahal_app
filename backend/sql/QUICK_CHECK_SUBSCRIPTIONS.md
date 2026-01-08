# Quick Queries to Check Subscriptions Table

## Access H2 Console

1. **Make sure backend is running**
2. Open browser: `http://localhost:8080/h2-console`
3. Login with:
   - **JDBC URL:** `jdbc:h2:file:./data/mahal_db`
   - **Username:** `sa`
   - **Password:** (leave empty)

## Quick Check Queries

### 1. See All Subscriptions
```sql
SELECT * FROM subscriptions ORDER BY created_at DESC;
```

### 2. See Subscriptions with Email
```sql
SELECT id, user_id, user_email, plan_duration, status, start_date, end_date, created_at 
FROM subscriptions 
ORDER BY created_at DESC;
```

### 3. Count Subscriptions
```sql
SELECT COUNT(*) AS total FROM subscriptions;
```

### 4. Check by Status
```sql
SELECT status, COUNT(*) AS count FROM subscriptions GROUP BY status;
```

### 5. Check if user_email Column Exists
```sql
SELECT id, user_id, user_email, status FROM subscriptions LIMIT 5;
```

If you get an error about `user_email` column not existing, the column hasn't been added yet. In that case, restart the backend - Hibernate will auto-add it.

### 6. See Active Subscriptions Only
```sql
SELECT id, user_email, plan_duration, status, end_date 
FROM subscriptions 
WHERE status = 'active';
```

### 7. Check Subscriptions by User Email
```sql
SELECT user_email, COUNT(*) AS subscription_count, MAX(status) AS latest_status
FROM subscriptions 
WHERE user_email IS NOT NULL
GROUP BY user_email;
```

## If Table Doesn't Exist

If you get an error "Table SUBSCRIPTIONS not found", the table will be created automatically when:
- The backend starts
- A subscription is created via the API
- Hibernate detects the Subscription entity and creates the table

You can also manually check table structure:
```sql
SHOW TABLES;
SHOW COLUMNS FROM subscriptions;
```

