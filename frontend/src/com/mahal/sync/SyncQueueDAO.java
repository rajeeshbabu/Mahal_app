package com.mahal.sync;

import com.mahal.database.DatabaseService;
import com.mahal.sync.SyncOperation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO for managing sync queue operations.
 * Stores operations that need to be synced to Supabase when internet is
 * available.
 */
public class SyncQueueDAO {
    private DatabaseService dbService;

    public SyncQueueDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS sync_queue (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "table_name TEXT NOT NULL, " +
                "operation TEXT NOT NULL, " + // INSERT, UPDATE, DELETE
                "record_id INTEGER, " +
                "data TEXT, " + // JSON string of the record data
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "synced_at TEXT, " +
                "sync_status TEXT DEFAULT 'PENDING', " + // PENDING, SYNCING, SYNCED, FAILED
                "retry_count INTEGER DEFAULT 0" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
        } catch (Exception e) {
            System.err.println("Error creating sync_queue table: " + e.getMessage());
        }
    }

    public Long queueOperation(String tableName, String operation, Long recordId, String data) {
        String sql = "INSERT INTO sync_queue (table_name, operation, record_id, data, sync_status) " +
                "VALUES (?, ?, ?, ?, 'PENDING')";
        Object[] params = { tableName, operation, recordId, data };
        return dbService.executeInsert(sql, params);
    }

    /**
     * Check if a specific operation for a record is already in the queue (any
     * status).
     */
    public boolean isOperationQueued(String tableName, String operation, Long recordId) {
        String sql = "SELECT COUNT(*) FROM sync_queue WHERE table_name = ? AND operation = ? AND record_id = ?";
        List<Integer> result = dbService.executeQuery(sql, new Object[] { tableName, operation, recordId }, rs -> {
            try {
                return rs.getInt(1);
            } catch (SQLException e) {
                return 0;
            }
        });
        return !result.isEmpty() && result.get(0) > 0;
    }

    /**
     * Get all pending sync operations.
     */
    public List<SyncOperation> getPendingOperations() {
        String sql = "SELECT id, table_name, operation, record_id, data, created_at, retry_count " +
                "FROM sync_queue " +
                "WHERE sync_status = 'PENDING' OR (sync_status = 'FAILED' AND retry_count < 5) " +
                "ORDER BY created_at ASC";
        return dbService.executeQuery(sql, rs -> {
            try {
                return mapResultSet(rs);
            } catch (SQLException e) {
                System.err.println("Error mapping sync operation: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Get all failed sync operations (for diagnostics).
     */
    public List<SyncOperation> getFailedOperations() {
        String sql = "SELECT id, table_name, operation, record_id, data, created_at, retry_count " +
                "FROM sync_queue " +
                "WHERE sync_status = 'FAILED' " +
                "ORDER BY created_at ASC";
        return dbService.executeQuery(sql, rs -> {
            try {
                return mapResultSet(rs);
            } catch (SQLException e) {
                System.err.println("Error mapping sync operation: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Reset failed operations back to PENDING so they can be retried.
     */
    public void resetFailedOperations() {
        String sql = "UPDATE sync_queue SET sync_status = 'PENDING', retry_count = 0 WHERE sync_status = 'FAILED'";
        int updated = dbService.executeUpdate(sql, null);
        System.out.println("Reset " + updated + " failed operations back to PENDING");
    }

    /**
     * Mark an operation as synced.
     */
    public void markAsSynced(Long queueId) {
        String sql = "UPDATE sync_queue SET sync_status = 'SYNCED', synced_at = datetime('now') WHERE id = ?";
        dbService.executeUpdate(sql, new Object[] { queueId });
    }

    /**
     * Mark an operation as failed and increment retry count.
     */
    public void markAsFailed(Long queueId) {
        String sql = "UPDATE sync_queue SET sync_status = 'FAILED', retry_count = retry_count + 1 WHERE id = ?";
        dbService.executeUpdate(sql, new Object[] { queueId });
    }

    /**
     * Mark an operation as syncing.
     */
    public void markAsSyncing(Long queueId) {
        String sql = "UPDATE sync_queue SET sync_status = 'SYNCING' WHERE id = ?";
        dbService.executeUpdate(sql, new Object[] { queueId });
    }

    /**
     * Clean up old synced operations (older than 30 days).
     */
    public void cleanupOldSyncedOperations() {
        String sql = "DELETE FROM sync_queue WHERE sync_status = 'SYNCED' AND " +
                "datetime(synced_at) < datetime('now', '-30 days')";
        dbService.executeUpdate(sql, null);
    }

    /**
     * Clear all pending and failed sync operations.
     * Useful when sync logic changes and old queue entries need to be re-queued.
     */
    public void clearPendingAndFailedOperations() {
        String sql = "DELETE FROM sync_queue WHERE sync_status IN ('PENDING', 'FAILED', 'SYNCING')";
        int deleted = dbService.executeUpdate(sql, null);
        System.out.println("Cleared " + deleted + " pending/failed sync queue operations");
    }

    private SyncOperation mapResultSet(ResultSet rs) throws SQLException {
        SyncOperation op = new SyncOperation();
        op.setId(rs.getLong("id"));
        op.setTableName(rs.getString("table_name"));
        op.setOperation(rs.getString("operation"));
        op.setRecordId(rs.getLong("record_id"));
        op.setData(rs.getString("data"));
        op.setCreatedAt(rs.getString("created_at"));
        op.setRetryCount(rs.getInt("retry_count"));
        return op;
    }
}
