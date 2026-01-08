# User Isolation Fix - Critical Security Update

## Problem
Users were seeing data from other users because sync operations didn't filter by `user_id`. When "rajeesh" logged in, they saw "sam's" data.

## Solution Implemented

### 1. Updated `SupabaseSyncService` Methods
All sync methods now require and use `user_id`:
- `insert(tableName, jsonData, userId)` - Adds `user_id` to JSON payload
- `update(tableName, recordId, jsonData, userId)` - Filters by `id AND user_id`
- `delete(tableName, recordId, userId)` - Filters by `id AND user_id`

### 2. Updated `SyncManager.queueOperation`
- Gets current user from `SessionManager`
- Adds `user_id` to JSON data before queuing
- Prevents sync if no user is logged in

### 3. Updated `SyncManager.syncPendingOperations`
- Extracts `user_id` from queued JSON data
- Passes `user_id` to all sync service methods

## CRITICAL: Additional Steps Required

### Step 1: Add `user_id` Column to All Tables in Supabase

You MUST add `user_id` column to every table in Supabase:

```sql
-- Example for members table
ALTER TABLE members ADD COLUMN user_id TEXT;

-- Repeat for ALL tables:
-- incomes, expenses, due_collections, inventory_items, events, 
-- rents, staff, committees, etc.
```

### Step 2: Enable Row Level Security (RLS) in Supabase

**CRITICAL**: Without RLS policies, users can still access each other's data even with `user_id` filtering.

```sql
-- Enable RLS on all tables
ALTER TABLE members ENABLE ROW LEVEL SECURITY;
ALTER TABLE incomes ENABLE ROW LEVEL SECURITY;
-- ... repeat for all tables

-- Create policies for each table (example for members)
CREATE POLICY "Users can only see own members"
    ON members FOR SELECT
    USING (auth.uid()::text = user_id);

CREATE POLICY "Users can only insert own members"
    ON members FOR INSERT
    WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "Users can only update own members"
    ON members FOR UPDATE
    USING (auth.uid()::text = user_id)
    WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "Users can only delete own members"
    ON members FOR DELETE
    USING (auth.uid()::text = user_id);
```

**Repeat these policies for ALL tables.**

### Step 3: Add `user_id` to Local SQLite Tables

You need to add `user_id` column to all local tables:

```sql
-- Example migration script
ALTER TABLE members ADD COLUMN user_id TEXT;
ALTER TABLE incomes ADD COLUMN user_id TEXT;
ALTER TABLE expenses ADD COLUMN user_id TEXT;
-- ... repeat for all tables
```

### Step 4: Update DAOs to Filter by `user_id`

All DAO queries must filter by `user_id`:

```java
// Example: MemberDAO.getAll()
public List<Member> getAll() {
    String userId = String.valueOf(SessionManager.getInstance().getCurrentUser().getId());
    String sql = "SELECT * FROM members WHERE user_id = ? ORDER BY name";
    return dbService.executeQuery(sql, new Object[]{userId}, this::mapResultSet);
}
```

### Step 5: Set `user_id` When Creating Records

All DAO `create()` methods must set `user_id`:

```java
// Example: MemberDAO.create()
public Long create(Member member) {
    String userId = String.valueOf(SessionManager.getInstance().getCurrentUser().getId());
    String sql = "INSERT INTO members (user_id, name, ...) VALUES (?, ?, ...)";
    Object[] params = {userId, member.getName(), ...};
    // ...
}
```

## Testing User Isolation

1. **Login as "sam"** → Create some records → Logout
2. **Login as "rajeesh"** → Should see NO records from "sam"
3. **Create records as "rajeesh"** → Should only see "rajeesh's" records
4. **Login as "sam" again** → Should only see "sam's" records

## Current Status

✅ **Fixed**: Sync operations now include `user_id`
⚠️ **Action Required**: 
- Add `user_id` columns to Supabase tables
- Enable RLS policies in Supabase
- Add `user_id` columns to local SQLite tables
- Update DAOs to filter by `user_id`

## Why This Matters

Without these changes:
- Users can still see each other's data in the local database
- Supabase won't enforce user isolation without RLS
- Data security is compromised

**This is a CRITICAL security issue. Complete all steps above before deploying to production.**
