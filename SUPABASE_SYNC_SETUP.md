# Supabase Sync Setup Guide

This application includes automatic synchronization between the local SQLite database and Supabase. When internet is available, changes are automatically synced. When offline, operations are queued and synced when connection is restored.

## Features

- **Automatic Sync**: Syncs data when internet connection is available
- **Offline Queue**: Queues operations when offline and syncs when connection is restored
- **Connectivity Detection**: Automatically detects internet connectivity changes
- **Retry Logic**: Failed syncs are retried automatically (up to 5 times)

## Setup Instructions

### 1. Configure Supabase

You need to configure your Supabase project URL and API key. This can be done programmatically:

```java
import com.mahal.sync.SyncHelper;

// Configure Supabase connection
SyncHelper.configureSupabase(
    "https://your-project-id.supabase.co",
    "your-supabase-anon-key"
);
```

Or create a `supabase.properties` file in the application root directory:

```properties
supabase.url=https://your-project-id.supabase.co
supabase.key=your-supabase-anon-key
```

### 2. Supabase Database Schema

Ensure your Supabase database has tables matching your local SQLite schema. The sync service will use the same table names and column names.

**Important**: Your Supabase tables should have:
- An `id` column (primary key)
- Columns matching your local database schema
- Row Level Security (RLS) policies configured appropriately

### 3. Integrating Sync into DAOs

To enable sync for a DAO, add sync calls after database operations:

```java
import com.mahal.sync.SyncHelper;

public class YourDAO {
    public Long create(YourModel model) {
        // ... existing create logic ...
        Long id = dbService.executeInsert(sql, params);
        
        // Queue for sync
        if (id != null) {
            SyncHelper.queueInsert("your_table_name", id, model);
        }
        
        return id;
    }
    
    public boolean update(YourModel model) {
        // ... existing update logic ...
        boolean success = dbService.executeUpdate(sql, params) > 0;
        
        // Queue for sync
        if (success) {
            SyncHelper.queueUpdate("your_table_name", model.getId(), model);
        }
        
        return success;
    }
    
    public boolean delete(Long id) {
        // ... existing delete logic ...
        boolean success = dbService.executeUpdate(sql, params) > 0;
        
        // Queue for sync
        if (success) {
            SyncHelper.queueDelete("your_table_name", id);
        }
        
        return success;
    }
}
```

### 4. Manual Sync Trigger

You can manually trigger sync from your UI:

```java
import com.mahal.sync.SyncHelper;

// Trigger sync manually
SyncHelper.triggerSync();
```

## How It Works

1. **When Online**: Operations are synced immediately to Supabase
2. **When Offline**: Operations are queued in the local `sync_queue` table
3. **Connection Restored**: All queued operations are automatically synced
4. **Periodic Sync**: The system checks for pending operations every 60 seconds when online

## Sync Queue Table

The sync system creates a `sync_queue` table in your local database to track pending operations:

- `id`: Queue entry ID
- `table_name`: Name of the table to sync
- `operation`: INSERT, UPDATE, or DELETE
- `record_id`: ID of the record
- `data`: JSON string of the record data
- `sync_status`: PENDING, SYNCING, SYNCED, or FAILED
- `retry_count`: Number of retry attempts

## Troubleshooting

### Sync Not Working

1. **Check Configuration**: Ensure Supabase URL and API key are set correctly
2. **Check Connectivity**: Verify internet connection is available
3. **Check Logs**: Look for error messages in console output
4. **Check Supabase**: Verify your Supabase project is accessible and tables exist

### Sync Failures

- Failed operations are retried automatically (up to 5 times)
- Check the `sync_queue` table for operations with `sync_status = 'FAILED'`
- Verify your Supabase table schema matches your local schema
- Check Supabase RLS policies allow the operations

## Security Notes

- Store Supabase credentials securely
- Use environment variables or encrypted config files in production
- Configure appropriate RLS policies in Supabase
- Consider using service role key for server-side operations (not recommended for client apps)

## Example: Complete DAO Integration

```java
package com.mahal.database;

import com.mahal.model.Income;
import com.mahal.sync.SyncHelper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class IncomeDAO {
    private DatabaseService dbService;
    
    public IncomeDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }
    
    public Long create(Income income) {
        String sql = "INSERT INTO incomes (...) VALUES (...)";
        Object[] params = {...};
        Long id = dbService.executeInsert(sql, params);
        
        if (id != null) {
            income.setId(id);
            SyncHelper.queueInsert("incomes", id, income);
        }
        
        return id;
    }
    
    public boolean update(Income income) {
        String sql = "UPDATE incomes SET ... WHERE id = ?";
        Object[] params = {...};
        boolean success = dbService.executeUpdate(sql, params) > 0;
        
        if (success) {
            SyncHelper.queueUpdate("incomes", income.getId(), income);
        }
        
        return success;
    }
    
    public boolean delete(Long id) {
        String sql = "DELETE FROM incomes WHERE id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[]{id}) > 0;
        
        if (success) {
            SyncHelper.queueDelete("incomes", id);
        }
        
        return success;
    }
}
```

## Next Steps

1. Configure your Supabase project
2. Add sync calls to your DAOs
3. Test with internet connection on/off
4. Monitor sync queue for any issues
5. Adjust sync intervals if needed (modify `SYNC_INTERVAL_SECONDS` in `SyncManager`)
