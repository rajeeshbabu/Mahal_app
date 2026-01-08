package com.mahal.util;

import com.mahal.database.*;
import com.mahal.sync.SyncHelper;
import com.mahal.sync.SyncManager;
import com.mahal.sync.SyncQueueDAO;
import com.mahal.sync.SyncOperation;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility to find and sync missing records from local DB to Supabase.
 * This will:
 * 1. Check for failed sync operations
 * 2. Re-queue all existing records for the current user
 * 3. Attempt to sync them
 */
public class FindAndSyncMissingRecords {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Find and Sync Missing Records Tool");
        System.out.println("========================================");
        
        // Get current user
        SessionManager sessionManager = SessionManager.getInstance();
        com.mahal.model.User currentUser = sessionManager.getCurrentUser();
        
        if (currentUser == null || currentUser.getId() == null) {
            System.err.println("ERROR: No user logged in. Please log in first.");
            return;
        }
        
        String userId = String.valueOf(currentUser.getId());
        System.out.println("Current user: " + currentUser.getFullName() + " (ID: " + userId + ")");
        System.out.println();
        
        SyncQueueDAO syncQueueDAO = new SyncQueueDAO();
        
        // Step 1: Check sync queue for failed operations
        checkFailedOperations();
        
        // Reset failed operations so they can be retried
        System.out.println("\nResetting failed operations for retry...");
        syncQueueDAO.resetFailedOperations();
        
        // Step 2: Count local records
        countLocalRecords(userId);
        
        // Step 3: Clear old sync queue and re-queue all records
        System.out.println("\n========================================");
        System.out.println("Step 3: Re-queueing all records for sync...");
        System.out.println("========================================");
        syncQueueDAO.clearPendingAndFailedOperations();
        
        // Perform initial sync (this will queue all records for current user)
        System.out.println("Re-queueing all existing records for user_id: " + userId);
        SyncHelper.performInitialSync();
        
        // Wait a bit for queueing to complete
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Step 4: Attempt to sync all pending operations
        System.out.println("\n========================================");
        System.out.println("Step 4: Syncing all queued operations...");
        System.out.println("========================================");
        
        List<SyncOperation> pendingOps = syncQueueDAO.getPendingOperations();
        System.out.println("Found " + pendingOps.size() + " operations to sync");
        
        if (pendingOps.size() > 0) {
            System.out.println("Starting sync...");
            SyncManager.getInstance().syncPendingOperations();
            
            // Wait for sync to complete
            try {
                Thread.sleep(10000); // Wait 10 seconds for sync
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Check results
            List<SyncOperation> stillPending = syncQueueDAO.getPendingOperations();
            System.out.println("\nSync completed. Still pending: " + stillPending.size() + " operations");
            
            if (stillPending.size() > 0) {
                System.out.println("\nSome operations are still pending. Check console logs for errors.");
                System.out.println("Pending operations by table:");
                Map<String, Integer> tableCounts = new HashMap<>();
                for (SyncOperation op : stillPending) {
                    tableCounts.put(op.getTableName(), tableCounts.getOrDefault(op.getTableName(), 0) + 1);
                }
                for (Map.Entry<String, Integer> entry : tableCounts.entrySet()) {
                    System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
                }
            }
        } else {
            System.out.println("No operations to sync. All records may already be synced.");
        }
        
        System.out.println("\n========================================");
        System.out.println("Process complete. Check Supabase to verify records.");
        System.out.println("========================================");
    }
    
    private static void checkFailedOperations() {
        System.out.println("Step 1: Checking for failed sync operations...");
        try {
            SyncQueueDAO syncQueueDAO = new SyncQueueDAO();
            DatabaseService dbService = DatabaseService.getInstance();
            
            // Count failed operations
            String sql = "SELECT COUNT(*) as count FROM sync_queue WHERE sync_status = 'FAILED'";
            List<Integer> results = dbService.executeQuery(sql, rs -> {
                try {
                    return rs.getInt("count");
                } catch (Exception e) {
                    return 0;
                }
            });
            
            int failedCount = results.isEmpty() ? 0 : results.get(0);
            System.out.println("  Failed operations: " + failedCount);
            
            if (failedCount > 0) {
                // Get failed operations by table
                sql = "SELECT table_name, COUNT(*) as count FROM sync_queue WHERE sync_status = 'FAILED' GROUP BY table_name";
                List<String> failedByTable = dbService.executeQuery(sql, rs -> {
                    try {
                        return rs.getString("table_name") + ": " + rs.getInt("count") + " failed";
                    } catch (Exception e) {
                        return "";
                    }
                });
                
                System.out.println("  Failed operations by table:");
                for (String info : failedByTable) {
                    if (!info.isEmpty()) {
                        System.out.println("    - " + info);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error checking failed operations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void countLocalRecords(String userId) {
        System.out.println("\nStep 2: Counting local records for user_id: " + userId);
        System.out.println("  (This shows how many records should be synced)");
        
        try {
            DatabaseService dbService = DatabaseService.getInstance();
            
            String[] tables = {
                "members", "incomes", "expenses", "due_collections", "masjids",
                "committees", "staff", "staff_salaries", "income_types", "due_types",
                "events", "inventory_items", "houses", "damaged_items", "rent_items",
                "rents", "marriage_certificates", "death_certificates", 
                "jamath_certificates", "custom_certificates"
            };
            
            System.out.println("\n  Record counts by table:");
            for (String table : tables) {
                try {
                    String sql = "SELECT COUNT(*) as count FROM " + table + " WHERE user_id = ?";
                    List<Integer> results = dbService.executeQuery(sql, new Object[]{userId}, rs -> {
                        try {
                            return rs.getInt("count");
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                    
                    int count = results.isEmpty() ? 0 : results.get(0);
                    if (count > 0) {
                        System.out.println("    - " + table + ": " + count + " records");
                    }
                } catch (Exception e) {
                    // Table might not exist or have user_id column yet
                    System.out.println("    - " + table + ": error (" + e.getMessage() + ")");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error counting local records: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
