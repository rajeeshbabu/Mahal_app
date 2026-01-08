# Adding Users to Backend H2 Database (Local)

## Overview

This guide explains how to add your two registered users to the backend H2 database.

## Prerequisites

1. **Get user data from SQLite database:**
   - Open `frontend/mahal.db` using SQLite Browser or command line
   - Run: `SELECT id, name, password, full_name, active FROM admins ORDER BY id;`
   - Copy the output (you'll need the email, password hash, and full_name)

## Steps

### Step 1: Open H2 Console

1. Start your backend application
2. Open browser: `http://localhost:8080/h2-console`
3. Login with:
   - **JDBC URL:** `jdbc:h2:file:./data/mahal_db`
   - **Username:** `sa`
   - **Password:** (leave empty)

### Step 2: Create Admins Table (if it doesn't exist)

Run this SQL:

```sql
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    active INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Step 3: Insert Your Users

Replace the placeholder values with your actual user data:

```sql
INSERT INTO admins (id, name, password, full_name, active, created_at, updated_at)
VALUES 
    (1, 'user1@example.com', '$2a$10$REPLACE_WITH_ACTUAL_HASH', 'User One Name', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'user2@example.com', '$2a$10$REPLACE_WITH_ACTUAL_HASH', 'User Two Name', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

**Important:**
- Replace `user1@example.com` and `user2@example.com` with actual emails
- Replace `$2a$10$REPLACE_WITH_ACTUAL_HASH` with the actual BCrypt password hash from your SQLite database
- Replace `User One Name` and `User Two Name` with actual full names
- Use the exact password hash from SQLite - do NOT re-hash the password

### Step 4: Verify

Check that users were inserted:

```sql
SELECT id, name, full_name, active, created_at FROM admins ORDER BY id;
```

## Example

If your SQLite database has:
```
1|sam@example.com|$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92dFmOiQ6wzO|Sam User|1
2|rajeesh@example.com|$2a$10$IjZAgcfl7p92dFmOiQ6wzON9qo8uLOickgx2ZMRZoMye|Rajeesh User|1
```

Then use:

```sql
INSERT INTO admins (id, name, password, full_name, active, created_at, updated_at)
VALUES 
    (1, 'sam@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92dFmOiQ6wzO', 'Sam User', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'rajeesh@example.com', '$2a$10$IjZAgcfl7p92dFmOiQ6wzON9qo8uLOickgx2ZMRZoMye', 'Rajeesh User', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

## Notes

- **Password Hashes:** Must be the exact BCrypt hash from your SQLite database
- **IDs:** Use the same IDs as in your SQLite database (1, 2, etc.) for consistency
- **Email:** The `name` column stores the email address (used for login)
- **Active:** Set to 1 for active users, 0 for inactive

## Alternative: Using the SQL File

You can also use the provided SQL file:
1. Open `backend/sql/ADD_USERS_TO_LOCAL_H2.sql`
2. Replace the placeholder values with your actual user data
3. Copy and paste into H2 Console
4. Run it

