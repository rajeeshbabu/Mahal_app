package com.mahal.sync;

import com.mahal.database.DatabaseService;
import java.time.Instant;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO for managing sync metadata:
 * - lastSyncTime per user
 * - record updated_at timestamps
 * - sync status flags
 */
public class SyncMetadataDAO {
    private DatabaseService dbService;
    
    public SyncMetadataDAO() {
        this.dbService = DatabaseService.getInstance();
        createTablesIfNotExist();
    }
    
    private void createTablesIfNotExist() {
        // Table for tracking last sync time per user
        String sql1 = "CREATE TABLE IF NOT EXISTS sync_metadata (" +
                     "user_id TEXT NOT NULL PRIMARY KEY, " +
                     "last_sync_time TEXT, " + // ISO 8601 UTC timestamp
                     "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                     ")";
        
        // Table for tracking record-level sync metadata
        String sql2 = "CREATE TABLE IF NOT EXISTS record_sync_metadata (" +
                     "table_name TEXT NOT NULL, " +
                     "record_id TEXT NOT NULL, " +
                     "user_id TEXT NOT NULL, " +
                     "updated_at TEXT, " + // ISO 8601 UTC timestamp
                     "is_synced INTEGER DEFAULT 0, " + // 0 = false, 1 = true
                     "sync_version INTEGER DEFAULT 0, " +
                     "PRIMARY KEY (table_name, record_id, user_id)" +
                     ")";
        
        try {
            dbService.executeUpdate(sql1, null);
            dbService.executeUpdate(sql2, null);
        } catch (Exception e) {
            System.err.println("Error creating sync metadata tables: " + e.getMessage());
        }
    }
    
    /**
     * Get last sync time for a user.
     */
    public Instant getLastSyncTime(String userId) {
        String sql = "SELECT last_sync_time FROM sync_metadata WHERE user_id = ?";
        try {
            var results = dbService.executeQuery(sql, new Object[]{userId}, rs -> {
                try {
                    String timeStr = rs.getString("last_sync_time");
                    return timeStr != null ? Instant.parse(timeStr) : null;
                } catch (Exception e) {
                    return null;
                }
            });
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Set last sync time for a user.
     */
    public void setLastSyncTime(String userId, Instant time) {
        String sql = "INSERT OR REPLACE INTO sync_metadata (user_id, last_sync_time, updated_at) " +
                    "VALUES (?, ?, datetime('now'))";
        dbService.executeUpdate(sql, new Object[]{userId, time.toString()});
    }
    
    /**
     * Get record's updated_at timestamp.
     */
    public Instant getRecordUpdatedAt(String tableName, String recordId) {
        String sql = "SELECT updated_at FROM record_sync_metadata " +
                    "WHERE table_name = ? AND record_id = ?";
        try {
            var results = dbService.executeQuery(sql, new Object[]{tableName, recordId}, rs -> {
                try {
                    String timeStr = rs.getString("updated_at");
                    return timeStr != null ? Instant.parse(timeStr) : null;
                } catch (Exception e) {
                    return null;
                }
            });
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Mark record as synced.
     */
    public void markAsSynced(String tableName, String recordId, String userId, Instant updatedAt) {
        String sql = "INSERT OR REPLACE INTO record_sync_metadata " +
                    "(table_name, record_id, user_id, updated_at, is_synced, sync_version) " +
                    "VALUES (?, ?, ?, ?, 1, COALESCE((SELECT sync_version FROM record_sync_metadata " +
                    "WHERE table_name = ? AND record_id = ? AND user_id = ?), 0) + 1)";
        dbService.executeUpdate(sql, new Object[]{
            tableName, recordId, userId, updatedAt.toString(),
            tableName, recordId, userId
        });
    }
    
    /**
     * Mark record as not synced (local changes pending).
     */
    public void markAsUnsynced(String tableName, String recordId, String userId) {
        String sql = "UPDATE record_sync_metadata SET is_synced = 0 " +
                    "WHERE table_name = ? AND record_id = ? AND user_id = ?";
        dbService.executeUpdate(sql, new Object[]{tableName, recordId, userId});
    }
}
