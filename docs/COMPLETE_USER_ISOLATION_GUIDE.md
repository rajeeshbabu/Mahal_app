# Complete User Isolation Implementation Guide

## ✅ What Has Been Fixed

### 1. Sync Layer (Complete)
- ✅ `SupabaseSyncService`: All methods now require and use `user_id`
- ✅ `SyncManager.queueOperation()`: Adds `user_id` from session to all queued operations
- ✅ `SyncManager.syncPendingOperations()`: Extracts and uses `user_id` from queued data

### 2. DAO Layer (Partially Complete)
- ✅ `MemberDAO`: Fully updated with user_id filtering
- ✅ `IncomeDAO`: Fully updated with user_id filtering
- ✅ `ExpenseDAO`: Fully updated with user_id filtering
- ⚠️ **Remaining DAOs need to be updated** (see pattern below)

### 3. Database Schema (Migration Scripts Created)
- ✅ SQL migration script for local SQLite: `sql/migrate_add_user_id_local.sql`
- ✅ SQL migration script for Supabase: `sql/migrate_add_user_id_supabase.sql`

## 📋 Remaining Tasks

### Task 1: Run SQL Migration Scripts

#### For Local SQLite Database:
```bash
# Option 1: Using SQLite CLI
sqlite3 mahal.db < sql/migrate_add_user_id_local.sql

# Option 2: Run in your application startup (one-time migration)
```

#### For Supabase:
1. Go to Supabase Dashboard → SQL Editor
2. Copy contents of `sql/migrate_add_user_id_supabase.sql`
3. Run the script

### Task 2: Update Remaining DAOs

You need to update these DAOs following the same pattern as `MemberDAO`, `IncomeDAO`, and `ExpenseDAO`:

**Priority DAOs** (most commonly used):
- `DueCollectionDAO`
- `InventoryItemDAO`
- `EventDAO`
- `RentDAO`
- `MasjidDAO`
- `StaffDAO`
- `CommitteeDAO`

**Other DAOs**:
- `DueTypeDAO`
- `IncomeTypeDAO`
- `DamagedItemDAO`
- `RentItemDAO`
- `StaffSalaryDAO`
- `HouseDAO`
- `CertificateDAO` (has multiple tables: marriage, death, jamath, custom)
- `PrayerTimeDAO` (optional - may be shared)

### Pattern for Updating Each DAO

#### Step 1: Update Table Creation
```java
private void createTableIfNotExists() {
    String sql = "CREATE TABLE IF NOT EXISTS table_name (" +
                 "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                 "user_id TEXT NOT NULL, " +  // ADD THIS LINE
                 // ... other columns
                 ")";
    try {
        dbService.executeUpdate(sql, null);
        DAOBase.ensureUserIdColumn(dbService, "table_name");  // ADD THIS
        try {
            dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_table_name_user_id ON table_name(user_id)", null);
        } catch (Exception e) {}
    } catch (Exception e) {
        System.err.println("Error creating table: " + e.getMessage());
    }
}
```

#### Step 2: Add getCurrentUserId() Helper Method
```java
private String getCurrentUserId() {
    SessionManager sessionManager = SessionManager.getInstance();
    User currentUser = sessionManager.getCurrentUser();
    if (currentUser == null || currentUser.getId() == null) {
        throw new IllegalStateException("No user logged in. Cannot perform database operation without user context.");
    }
    return String.valueOf(currentUser.getId());
}
```

#### Step 3: Update getAll() Method
```java
public List<Model> getAll() {
    String userId = getCurrentUserId();  // ADD THIS
    String sql = "SELECT id, user_id, ... FROM table_name WHERE user_id = ? ORDER BY ...";  // ADD user_id filter
    return dbService.executeQuery(sql, new Object[]{userId}, this::mapResultSet);  // ADD userId param
}
```

#### Step 4: Update getById() Method
```java
public Model getById(Long id) {
    String userId = getCurrentUserId();  // ADD THIS
    String sql = "SELECT id, user_id, ... FROM table_name WHERE id = ? AND user_id = ?";  // ADD user_id filter
    List<Model> results = dbService.executeQuery(sql, new Object[]{id, userId}, this::mapResultSet);
    return results.isEmpty() ? null : results.get(0);
}
```

#### Step 5: Update create() Method
```java
public Long create(Model model) {
    String userId = getCurrentUserId();  // ADD THIS
    String sql = "INSERT INTO table_name (user_id, col1, col2, ...) VALUES (?, ?, ?, ...)";  // ADD user_id
    Object[] params = {
        userId,  // ADD THIS as first parameter
        model.getCol1(),
        model.getCol2(),
        // ... other fields
    };
    Long newId = dbService.executeInsert(sql, params);
    // ... rest of method
}
```

#### Step 6: Update update() Method
```java
public boolean update(Model model) {
    String userId = getCurrentUserId();  // ADD THIS
    String sql = "UPDATE table_name SET col1 = ?, col2 = ?, ... WHERE id = ? AND user_id = ?";  // ADD user_id filter
    Object[] params = {
        model.getCol1(),
        model.getCol2(),
        // ... other fields
        model.getId(),
        userId  // ADD THIS as last parameter
    };
    boolean success = dbService.executeUpdate(sql, params) > 0;
    // ... rest of method
}
```

#### Step 7: Update delete() Method
```java
public boolean delete(Long id) {
    String userId = getCurrentUserId();  // ADD THIS
    String sql = "DELETE FROM table_name WHERE id = ? AND user_id = ?";  // ADD user_id filter
    boolean success = dbService.executeUpdate(sql, new Object[]{id, userId}) > 0;  // ADD userId param
    // ... rest of method
}
```

#### Step 8: Update mapResultSet() Method
```java
private Model mapResultSet(ResultSet rs) {
    try {
        Model model = new Model();
        model.setId(rs.getLong("id"));
        // ADD THIS if model has user_id field:
        // model.setUserId(rs.getString("user_id"));
        // ... map other fields
        return model;
    } catch (SQLException e) {
        // ... error handling
    }
}
```

## 🧪 Testing Checklist

After implementing user isolation:

1. **Test 1: Login as User A**
   - Create some records (members, incomes, expenses)
   - Verify records are created with correct user_id
   - Logout

2. **Test 2: Login as User B**
   - Should see NO records from User A
   - Create new records
   - Verify only User B's records are visible
   - Logout

3. **Test 3: Login as User A again**
   - Should see only User A's original records
   - Should NOT see User B's records

4. **Test 4: Try to access another user's record**
   - Attempt to update a record that belongs to User B (should fail silently or return false)
   - Attempt to delete a record that belongs to User B (should fail silently or return false)

5. **Test 5: Sync Verification**
   - Check Supabase dashboard
   - Verify records in Supabase have correct user_id
   - Verify RLS policies prevent cross-user access

## 🔒 Security Notes

1. **Never trust client input**: Always get `user_id` from `SessionManager`, never from request parameters
2. **Always filter by user_id**: Every query must include `WHERE user_id = ?`
3. **Double-check on updates/deletes**: Always use `WHERE id = ? AND user_id = ?`
4. **RLS is backup**: Even with RLS, application-layer filtering is essential
5. **Test thoroughly**: Security bugs are critical - test with multiple users

## 📝 Quick Reference: What Each DAO Needs

Each DAO needs these changes:
- ✅ Add `user_id TEXT NOT NULL` to table schema
- ✅ Call `DAOBase.ensureUserIdColumn()` in `createTableIfNotExists()`
- ✅ Add `getCurrentUserId()` helper method
- ✅ Update `getAll()` to filter by `user_id`
- ✅ Update `getById()` to filter by `user_id`
- ✅ Update all query methods to filter by `user_id`
- ✅ Update `create()` to set `user_id`
- ✅ Update `update()` to filter by `user_id`
- ✅ Update `delete()` to filter by `user_id`
- ✅ Add `user_id` to SELECT clauses if needed

## ⚠️ Important Notes

1. **Existing Data**: After adding `user_id` columns, existing records will have `NULL` user_id. You need to assign them to users or handle migration.

2. **JOIN Queries**: When joining tables, ensure both tables filter by `user_id`:
   ```sql
   LEFT JOIN masjids m ON i.masjid_id = m.id AND m.user_id = ?
   ```

3. **Indexes**: Indexes on `user_id` improve query performance.

4. **Supabase RLS**: The current RLS policies use `true` as placeholder. For production, implement proper JWT claim verification.

## 🚀 Deployment Checklist

Before deploying to production:
- [ ] All DAOs updated with user_id filtering
- [ ] SQL migration scripts run on both local and Supabase
- [ ] RLS policies enabled in Supabase
- [ ] Tested with multiple users
- [ ] Existing data migrated to have user_id
- [ ] Performance tested (indexes created)
- [ ] Error handling verified (no user logged in scenarios)
