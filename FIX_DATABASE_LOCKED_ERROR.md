# Fix: SQLite Database Locked Error

## Problem

You're seeing this error:
```
[SQLITE_BUSY] The database file is locked (database is locked)
```

This happens when multiple processes try to access the SQLite database at the same time.

## Solution Applied

I've updated the database connection URL to include `busy_timeout=5000`, which tells SQLite to wait up to 5 seconds if the database is locked, instead of immediately failing.

## Additional Steps to Fix

### Step 1: Close DB Browser for SQLite

**Most Common Cause:** If DB Browser for SQLite is open with the database, it locks the file.

1. **Close DB Browser for SQLite completely**
2. Make sure no database file is open in DB Browser
3. Restart your JavaFX application

### Step 2: Make Sure Only One Instance is Running

1. **Close all running instances** of your JavaFX application
2. **Open only one instance** at a time
3. SQLite doesn't handle multiple processes writing at the same time well

### Step 3: Restart the Application

After closing DB Browser and any duplicate instances:

1. Restart your JavaFX application
2. The database locking should be resolved

## Prevention

### Always Close DB Browser Before Running App

- **Before running the app:** Close DB Browser for SQLite
- **Before opening in DB Browser:** Close the JavaFX application

### Use DB Browser Only When App is Not Running

1. Stop the JavaFX application
2. Open DB Browser to view/edit data
3. Close DB Browser
4. Start the JavaFX application again

## Technical Details

SQLite uses file-level locking. When:
- DB Browser opens the database file → SQLite locks it
- Application tries to write → Gets "database is locked" error
- Multiple instances of the app → They compete for the lock

The `busy_timeout=5000` parameter helps by:
- Waiting up to 5 seconds if the database is locked
- Retrying the operation instead of failing immediately
- Reduces errors from brief lock conflicts

## Verification

After applying the fix and closing DB Browser:

1. Restart the JavaFX application
2. Try to register a new user or perform database operations
3. You should no longer see "database is locked" errors

