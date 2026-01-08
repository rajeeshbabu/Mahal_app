# Bidirectional Sync Guide

## Overview

The application now supports **bidirectional synchronization** between local SQLite database and Supabase:

1. **Local → Supabase (Sync Up)**: When you create, update, or delete records locally, they automatically sync to Supabase (existing functionality).
2. **Supabase → Local (Sync Down)**: When you create, update, or delete records directly in Supabase (via web UI or API), they automatically sync down to your local SQLite database (NEW).

## How It Works

### Sync Down Process

1. **Periodic Sync**: Every 60 seconds (when online), the app automatically:
   - Syncs local changes to Supabase (sync up)
   - Fetches changes from Supabase and updates local database (sync down)

2. **Initial Sync**: When you run initial sync, it also performs a sync down to get any remote changes.

3. **Conflict Resolution**: 
   - Uses `updated_at` timestamps to determine which version is newer
   - If Supabase has a newer version, local database is updated
   - If local has a newer version, local changes take precedence (and will sync up on next sync)

### Supported Tables

Currently, the following tables support bidirectional sync:

- ✅ **masjids** - Masjids/Mosques
- ✅ **committees** - Committee members
- ✅ **members** - Members (fetches but needs upsert method)
- ✅ **incomes** - Income records
- ✅ **expenses** - Expense records
- ✅ **due_collections** - Due collections
- ✅ **inventory_items** - Inventory items
- ✅ **damaged_items** - Damaged items
- ✅ **rent_items** - Rent items
- ✅ **rents** - Rent records
- ✅ **events** - Events
- ✅ **staff** - Staff records
- ✅ **staff_salaries** - Staff salaries
- ✅ **houses** - Houses
- ✅ **income_types** - Income types
- ✅ **due_types** - Due types
- ✅ **marriage_certificates** - Marriage certificates
- ✅ **death_certificates** - Death certificates
- ✅ **jamath_certificates** - Jamath certificates
- ✅ **custom_certificates** - Custom certificates

**Note**: Only `masjids` and `committees` currently have full upsert implementation. Other tables will fetch data but need upsert methods added to their DAOs.

## Adding Upsert Support to Other Tables

To add bidirectional sync for additional tables, add an `upsertFromSupabase` method to the corresponding DAO class.

### Example: Adding Upsert to a DAO

```java
/**
 * Upsert a record from Supabase (insert if new, update if exists).
 * Does NOT queue for sync (to avoid sync loops).
 */
public boolean upsertFromSupabase(YourModel model, String supabaseUpdatedAt) {
    if (model == null || model.getId() == null) {
        return false;
    }
    
    String userId = getCurrentUserId();
    YourModel existing = getById(model.getId());
    
    if (existing != null) {
        // Update if Supabase is newer
        // ... (compare timestamps and update if needed)
    } else {
        // Insert new record
        // ... (insert with Supabase data)
    }

}

```

Then add a case in `SyncManager.upsertModelToLocal()` method to route to your DAO.

## Testing Bidirectional Sync

### Test Sync Down (Supabase → Local)

1. Log in to your Java application
2. Open Supabase Dashboard: https://supabase.com/dashboard/project/hkckhwxpxfylaeqnlrrv
3. Go to Table Editor
4. Select a table (e.g., `masjids`)
5. Add, update, or delete a record **for your user_id**
6. Wait up to 60 seconds (or trigger sync manually)
7. Check your local database - the change should appear

### Test Sync Up (Local → Supabase)

1. Create/update/delete a record in your Java application
2. It should automatically sync to Supabase within a few seconds
3. Verify in Supabase Dashboard

## Manual Sync Trigger

You can manually trigger sync down from code:

```java
SyncManager.getInstance().syncDownFromSupabase();
```

## Important Notes

1. **User Isolation**: Sync only works for records belonging to the currently logged-in user (filtered by `user_id`).

2. **Timestamp Comparison**: The system uses `updated_at` timestamps to resolve conflicts. The newer version wins.

3. **No Sync Loops**: Upsert methods do NOT queue records for sync back to Supabase, preventing infinite sync loops.

4. **Delete Operations**: Currently, deletions in Supabase are NOT automatically synced down (this is complex to track). Only inserts and updates are synced.

5. **Performance**: Sync down fetches all records for each table. For large datasets, this may take some time. Consider optimizing with incremental sync (fetch only records modified after last sync) in the future.

## Troubleshooting

### Records not syncing down

1. Check console logs for error messages
2. Verify user_id matches between Supabase and local database
3. Ensure `updated_at` timestamps are present in both databases
4. Check if upsert method exists for the table's DAO

### Sync conflicts

If you see conflicting changes:
- Check timestamps: newer version wins
- Local changes will sync up on next sync cycle
- Supabase changes will sync down on next sync cycle
- Final state will converge after both syncs complete

### Performance issues

- Sync down processes all tables sequentially
- Large tables may take time
- Consider adding incremental sync in future updates

