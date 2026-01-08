# Sync Architecture Best Practices & Common Mistakes

## ✅ Correct Architecture

### 1. UUID-based IDs
```java
// ✅ CORRECT: Generate UUID on client
String id = UuidUtil.generateUuid();
member.setId(id);

// ❌ WRONG: Using auto-increment IDs
// INTEGER PRIMARY KEY AUTOINCREMENT causes conflicts across clients
```

### 2. User Isolation
```java
// ✅ CORRECT: Always include user_id from JWT
String userId = JwtUtil.extractUserId(jwtToken);
member.setUserId(userId);

// Database query with user filter
SELECT * FROM members WHERE user_id = ? AND id = ?

// ❌ WRONG: No user_id filter - security vulnerability
SELECT * FROM members WHERE id = ?
```

### 3. Timestamp-based Conflict Resolution
```java
// ✅ CORRECT: Compare updated_at timestamps
if (cloudUpdatedAt.isAfter(localUpdatedAt)) {
    // Apply cloud version
} else if (localUpdatedAt.isAfter(cloudUpdatedAt)) {
    // Keep local version (will be pushed)
}

// ❌ WRONG: No conflict resolution - data loss risk
// Just overwrite without checking timestamps
```

### 4. Incremental Sync
```java
// ✅ CORRECT: Only sync records changed since last sync
Instant lastSyncTime = metadataDAO.getLastSyncTime(userId);
List<Record> records = downloadRecords(tableName, lastSyncTime, userId);

// ❌ WRONG: Always sync all records
List<Record> records = downloadAllRecords(tableName, userId);
```

### 5. Background Threading (JavaFX)
```java
// ✅ CORRECT: Run sync in background thread
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.execute(() -> {
    syncManager.performSync();
});

// ❌ WRONG: Run sync on UI thread - freezes UI
Platform.runLater(() -> {
    syncManager.performSync(); // BLOCKS UI!
});
```

### 6. Retry Logic with Exponential Backoff
```java
// ✅ CORRECT: Exponential backoff
long delay = 1000; // 1 second
for (int attempt = 0; attempt < maxRetries; attempt++) {
    try {
        return performSync();
    } catch (Exception e) {
        if (attempt < maxRetries - 1) {
            Thread.sleep(delay);
            delay *= 2; // Exponential backoff
        }
    }
}

// ❌ WRONG: Immediate retry or fixed delay
for (int attempt = 0; attempt < maxRetries; attempt++) {
    performSync(); // No delay = hammering server
}
```

### 7. JWT User Context
```java
// ✅ CORRECT: Extract user_id from JWT on every request
public void setJwtToken(String jwtToken) {
    UserContext.setJwtToken(jwtToken);
    String userId = JwtUtil.extractUserId(jwtToken);
    UserContext.setUserId(userId);
}

// All sync operations use user_id from context
syncService.upload(tableName, jsonData, UserContext.getUserId());

// ❌ WRONG: Using API key only - no user context
syncService.upload(tableName, jsonData); // No user_id = security risk
```

## ❌ Common Mistakes

### Mistake 1: Auto-increment IDs
**Problem**: Integer IDs conflict across clients.
```
Client A creates record → ID = 1
Client B creates record → ID = 1 (CONFLICT!)
```
**Solution**: Use UUIDs.

### Mistake 2: Missing user_id
**Problem**: All users see all data (security breach).
```sql
-- ❌ WRONG: No user_id filter
SELECT * FROM members WHERE id = ?
```
**Solution**: Always filter by user_id:
```sql
SELECT * FROM members WHERE user_id = ? AND id = ?
```

### Mistake 3: No conflict resolution
**Problem**: Last write overwrites, losing data.
```
Client A updates record at 10:00
Client B updates same record at 10:01
Client A syncs → overwrites Client B's changes (DATA LOSS!)
```
**Solution**: Last-write-wins using updated_at timestamps.

### Mistake 4: Full sync every time
**Problem**: Wastes bandwidth and time.
```java
// ❌ WRONG: Downloads all records every sync
List<Record> allRecords = getAllRecords();
```
**Solution**: Incremental sync using lastSyncTime.

### Mistake 5: Sync on UI thread
**Problem**: UI freezes during network operations.
```java
// ❌ WRONG: Blocks UI thread
button.setOnAction(e -> {
    syncManager.sync(); // UI freezes!
});
```
**Solution**: Run sync in background thread.

### Mistake 6: No retry logic
**Problem**: Temporary network failures cause permanent sync failures.
```java
// ❌ WRONG: Fails on first error
try {
    syncService.upload(data);
} catch (Exception e) {
    // Give up immediately
}
```
**Solution**: Exponential backoff retry.

### Mistake 7: Using API key instead of JWT
**Problem**: No user context, can't enforce user isolation.
```java
// ❌ WRONG: Only API key
conn.setRequestProperty("Authorization", "Bearer " + apiKey);
```
**Solution**: Use JWT token, extract user_id, filter by user_id.

### Mistake 8: Not updating updated_at on changes
**Problem**: Conflict resolution fails.
```java
// ❌ WRONG: updated_at never changes
member.setName("New Name");
// updated_at still old timestamp
```
**Solution**: Auto-update updated_at on every UPDATE.

### Mistake 9: Mixing local and cloud timestamps
**Problem**: Timezone issues cause incorrect conflict resolution.
```java
// ❌ WRONG: Using local timezone
member.setUpdatedAt(LocalDateTime.now());
```
**Solution**: Always use UTC (Instant).

### Mistake 10: No sync status tracking
**Problem**: Can't distinguish synced vs unsynced records.
```java
// ❌ WRONG: No way to know if record needs syncing
```
**Solution**: Use is_synced flag or sync metadata table.

## Security Checklist

- [ ] Every table has `user_id` column
- [ ] All queries filter by `user_id`
- [ ] RLS policies enabled in Supabase
- [ ] JWT token validated on every request
- [ ] user_id extracted from JWT (not trusted from client)
- [ ] No direct database access (use API)
- [ ] HTTPS only (no HTTP)
- [ ] JWT tokens expire (refresh token mechanism)

## Performance Checklist

- [ ] Indexes on `user_id` and `updated_at`
- [ ] Incremental sync (not full sync)
- [ ] Batch operations (not one-by-one)
- [ ] Background threading (not UI thread)
- [ ] Connection pooling
- [ ] Timeout settings (30s connect, 30s read)
- [ ] Retry with exponential backoff

## Testing Checklist

- [ ] Test with multiple users (ensure isolation)
- [ ] Test offline mode (queue operations)
- [ ] Test conflict resolution (same record updated by 2 clients)
- [ ] Test network failures (retry logic)
- [ ] Test large datasets (performance)
- [ ] Test slow connections (timeout handling)
- [ ] Test JWT expiration (refresh token)
