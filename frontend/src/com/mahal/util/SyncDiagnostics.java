package com.mahal.util;

import com.mahal.database.*;
import com.mahal.sync.SyncHelper;
import com.mahal.sync.SyncManager;
import com.mahal.sync.SyncQueueDAO;
import java.util.List;

/**
 * Diagnostic utility to find and sync missing records between local DB and Supabase.
 */
public class SyncDiagnostics {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Sync Diagnostics Tool");
        System.out.println("========================================");
        
        // Check sync queue status
        checkSyncQueue();
        
        // Force sync of pending operations
        System.out.println("\n========================================");
        System.out.println("Attempting to sync all pending operations...");
        System.out.println("========================================");
        SyncManager.getInstance().syncPendingOperations();
        
        // Wait a bit for sync to complete
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n========================================");
        System.out.println("If records are still missing, you may need to:");
        System.out.println("1. Log in as the user whose records are missing");
        System.out.println("2. Run initial sync: SyncHelper.performInitialSync()");
        System.out.println("3. Check console logs for any error messages");
        System.out.println("========================================");
    }
    
    private static void checkSyncQueue() {
        try {
            SyncQueueDAO syncQueueDAO = new SyncQueueDAO();
            
            // Get pending operations
            List<com.mahal.sync.SyncOperation> pendingOps = syncQueueDAO.getPendingOperations();
            System.out.println("\nPending sync operations: " + pendingOps.size());
            
            if (pendingOps.size() > 0) {
                System.out.println("\nPending operations by table:");
                java.util.Map<String, Integer> tableCounts = new java.util.HashMap<>();
                for (com.mahal.sync.SyncOperation op : pendingOps) {
                    tableCounts.put(op.getTableName(), tableCounts.getOrDefault(op.getTableName(), 0) + 1);
                }
                for (java.util.Map.Entry<String, Integer> entry : tableCounts.entrySet()) {
                    System.out.println("  - " + entry.getKey() + ": " + entry.getValue() + " operations");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error checking sync queue: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
