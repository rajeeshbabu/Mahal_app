package com.mahal.sync;

/**
 * Helper class to simplify syncing operations from DAOs.
 * Provides convenient methods to queue sync operations.
 */
public class SyncHelper {
    private static SyncManager syncManager = SyncManager.getInstance();

    /**
     * Queue an INSERT operation for sync.
     */
    public static void queueInsert(String tableName, Long recordId, Object data) {
        syncManager.queueOperation(tableName, "INSERT", recordId, data);
    }

    /**
     * Queue an UPDATE operation for sync.
     */
    public static void queueUpdate(String tableName, Long recordId, Object data) {
        syncManager.queueOperation(tableName, "UPDATE", recordId, data);
    }

    /**
     * Queue a DELETE operation for sync.
     */
    public static void queueDelete(String tableName, Long recordId) {
        // For delete, we just need the ID, so create a minimal object
        java.util.HashMap<String, Object> data = new java.util.HashMap<>();
        data.put("id", recordId);
        syncManager.queueOperation(tableName, "DELETE", recordId, data);
    }

    /**
     * Configure Supabase connection.
     */
    public static void configureSupabase(String url, String apiKey) {
        syncManager.configureSupabase(url, apiKey);
    }

    /**
     * Manually trigger sync.
     */
    public static void triggerSync() {
        syncManager.triggerSync();
    }

    /**
     * Perform initial sync of all existing data from local database to Supabase.
     * This will queue all existing records for sync.
     */
    public static void performInitialSync() {
        syncManager.performInitialSync();
    }
}
