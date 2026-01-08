# Creating Subscriptions Table in H2 Database

## Why the Table Doesn't Exist

The subscriptions table should be created automatically by Hibernate when the backend starts (because `spring.jpa.hibernate.ddl-auto=update` is set). However, if it doesn't exist, you can create it manually.

## Solution: Manual Creation

### Step 1: Open H2 Console

1. **Make sure backend is running**
2. Open browser: `http://localhost:8080/h2-console`
3. Login with:
   - **JDBC URL:** `jdbc:h2:file:./data/mahal_db`
   - **Username:** `sa`
   - **Password:** (leave empty)

### Step 2: Run the CREATE TABLE Script

Copy and paste this SQL into H2 Console:

```sql
-- Create subscriptions table
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255),
    plan_duration VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    razorpay_subscription_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_email ON subscriptions(user_email);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_razorpay_id ON subscriptions(razorpay_subscription_id);
```

### Step 3: Verify

Check that the table was created:

```sql
SHOW TABLES;
SELECT * FROM subscriptions LIMIT 1;
```

Or check the table structure:

```sql
SHOW COLUMNS FROM subscriptions;
```

## Alternative: Let Hibernate Create It

If you prefer to let Hibernate create it automatically:

1. **Restart the backend application**
2. Hibernate will detect the `Subscription` entity
3. It will automatically create the table with `spring.jpa.hibernate.ddl-auto=update`
4. The table will be created the next time the backend starts

However, if you want to create it now and start using it, use the manual method above.

## File Reference

The SQL script is also available in: `backend/sql/CREATE_SUBSCRIPTIONS_TABLE_H2.sql`

