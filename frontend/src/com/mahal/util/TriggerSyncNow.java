package com.mahal.util;

import com.mahal.sync.SyncHelper;

/**
 * Utility class to trigger sync immediately from command line or code.
 * Usage: java -cp "bin;lib/sqlite-jdbc.jar" com.mahal.util.TriggerSyncNow
 */
public class TriggerSyncNow {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Triggering Sync Now...");
        System.out.println("========================================");
        
        try {
            SyncHelper.triggerSync();
            System.out.println("Sync command sent. Check console for sync progress.");
            System.out.println("Syncing will happen in the background.");
        } catch (Exception e) {
            System.err.println("Error triggering sync: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Keep process alive briefly to see output
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Trigger sync from code.
     */
    public static void syncNow() {
        SyncHelper.triggerSync();
    }
}



