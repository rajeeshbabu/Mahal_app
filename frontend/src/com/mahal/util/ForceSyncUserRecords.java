package com.mahal.util;

import com.mahal.sync.SyncHelper;
import com.mahal.sync.SyncManager;
import com.mahal.database.DatabaseService;
import java.util.List;

/**
 * Force sync all records for the currently logged-in user.
 * Use this if records were created but not synced.
 */
public class ForceSyncUserRecords {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Force Sync User Records");
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
        
        // Step 1: Perform initial sync (queues all records)
        System.out.println("Step 1: Queuing all records for sync...");
        SyncHelper.performInitialSync();
        
        // Wait for queueing to complete
        try {
            Thread.sleep(5000); // Wait 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Step 2: Force sync
        System.out.println("\nStep 2: Forcing sync of all queued operations...");
        SyncManager.getInstance().syncPendingOperations();
        
        System.out.println("\nSync initiated. Check console logs for progress.");
        System.out.println("Wait a few seconds, then check Supabase to verify records are synced.");
    }
}
