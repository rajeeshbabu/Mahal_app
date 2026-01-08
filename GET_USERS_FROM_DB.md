# How to Get User Data from Local Database

## Quick Method: Using SQLite Command Line

If you have SQLite installed, run:

```bash
sqlite3 frontend/mahal.db "SELECT id, name, password, full_name, active, created_at FROM admins ORDER BY id;"
```

This will output something like:
```
1|user1@example.com|$2a$10$N9qo8uLOickgx2ZMRZoMye|User One|1|2025-01-01 10:00:00
2|user2@example.com|$2a$10$IjZAgcfl7p92dFmOiQ6wzO|User Two|1|2025-01-01 11:00:00
```

## Alternative: Using a SQLite Browser

1. Download DB Browser for SQLite (free): https://sqlitebrowser.org/
2. Open `frontend/mahal.db`
3. Go to "Execute SQL" tab
4. Run: `SELECT id, name, password, full_name, active, created_at FROM admins ORDER BY id;`
5. Copy the results

## Once You Have the Data

Copy the output and use it to fill in `backend/sql/INSERT_USERS_TEMPLATE.sql`, then run it in Supabase SQL Editor.

