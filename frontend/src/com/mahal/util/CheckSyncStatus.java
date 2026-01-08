package com.mahal.util;

import com.mahal.sync.SyncQueueDAO;
import com.mahal.sync.SyncOperation;
import com.mahal.sync.SyncManager;
import com.mahal.database.DatabaseService;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Quick utility to check sync status and see what's pending/failed.
 */
public class CheckSyncStatus {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Sync Status Check");
        System.out.println("========================================");
        
        // Check current user
        SessionManager sessionManager = SessionManager.getInstance();
        com.mahal.model.User currentUser = sessionManager.getCurrentUser();
        
        if (currentUser == null || currentUser.getId() == null) {
            System.err.println("ERROR: No user logged in.");
            return;
        }
        
        String userId = String.valueOf(currentUser.getId());
        System.out.println("Current user: " + currentUser.getFullName() + " (ID: " + userId + ")\n");
        
        // Check sync queue
        SyncQueueDAO syncQueueDAO = new SyncQueueDAO();
        
        // Get pending operations
        List<SyncOperation> pendingOps = syncQueueDAO.getPendingOperations();
        System.out.println("Pending sync operations: " + pendingOps.size());
        
        if (pendingOps.size() > 0) {
            System.out.println("\nPending operations by table:");
            Map<String, Integer> tableCounts = new HashMap<>();
            for (SyncOperation op : pendingOps) {
                tableCounts.put(op.getTableName(), tableCounts.getOrDefault(op.getTableName(), 0) + 1);
            }
            for (Map.Entry<String, Integer> entry : tableCounts.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }
            
            // Show first few operations details
            System.out.println("\nFirst 5 pending operations:");
            int count = 0;
            for (SyncOperation op : pendingOps) {
                if (count++ >= 5) break;
                System.out.println("  " + count + ". " + op.getOperation() + " on " + op.getTableName() + 
                                 " (record ID: " + op.getRecordId() + ")");
            }
        }
        
        // Get failed operations
        List<SyncOperation> failedOps = syncQueueDAO.getFailedOperations();
        System.out.println("\nFailed sync operations: " + failedOps.size());
        
        if (failedOps.size() > 0) {
            System.out.println("\nFailed operations by table:");
            Map<String, Integer> failedCounts = new HashMap<>();
            for (SyncOperation op : failedOps) {
                failedCounts.put(op.getTableName(), failedCounts.getOrDefault(op.getTableName(), 0) + 1);
            }
            for (Map.Entry<String, Integer> entry : failedCounts.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        // Check local records for committees and masjids
        System.out.println("\n========================================");
        System.out.println("Local records for user_id: " + userId);
        System.out.println("========================================");
        checkLocalRecords(userId);
        
        // Suggest next steps
        System.out.println("\n========================================");
        System.out.println("Next Steps:");
        System.out.println("========================================");
        if (pendingOps.size() > 0) {
            System.out.println("1. Run: SyncManager.getInstance().syncPendingOperations()");
            System.out.println("   Or restart the app to trigger automatic sync");
        } else if (failedOps.size() > 0) {
            System.out.println("1. Check console logs for error messages");
            System.out.println("2. Fix errors and reset failed operations:");
            System.out.println("   syncQueueDAO.resetFailedOperations()");
        } else {
            System.out.println("Sync queue is empty. If records are missing:");
            System.out.println("1. Run: SyncHelper.performInitialSync() to re-queue all records");
            System.out.println("2. Then run: SyncManager.getInstance().syncPendingOperations()");
        }
    }
    
    private static void checkLocalRecords(String userId) {
        try {
            DatabaseService dbService = DatabaseService.getInstance();
            
            // Check committees
            String sql = "SELECT COUNT(*) as count FROM committees WHERE user_id = ?";
            List<Integer> committeeResults = dbService.executeQuery(sql, new Object[]{userId}, rs -> {
                try {
                    return rs.getInt("count");
                } catch (Exception e) {
                    return 0;
                }
            });
            int committeeCount = committeeResults.isEmpty() ? 0 : committeeResults.get(0);
            System.out.println("Committees: " + committeeCount + " records");
            
            // Check masjids
            sql = "SELECT COUNT(*) as count FROM masjids WHERE user_id = ?";
            List<Integer> masjidResults = dbService.executeQuery(sql, new Object[]{userId}, rs -> {
                try {
                    return rs.getInt("count");
                } catch (Exception e) {
                    return 0;
                }
            });
            int masjidCount = masjidResults.isEmpty() ? 0 : masjidResults.get(0);
            System.out.println("Masjids: " + masjidCount + " records");
            
        } catch (Exception e) {
            System.err.println("Error checking local records: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
