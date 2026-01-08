// Quick utility to trigger sync immediately
// Run: java -cp "bin;lib/sqlite-jdbc.jar" SyncNow

import com.mahal.sync.SyncHelper;

public class SyncNow {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Triggering Sync Now...");
        System.out.println("========================================");
        
        try {
            SyncHelper.triggerSync();
            System.out.println("✓ Sync triggered successfully!");
            System.out.println("Check console output for sync progress.");
            System.out.println("Syncing will happen in the background.");
        } catch (Exception e) {
            System.err.println("✗ Error triggering sync: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Keep process alive briefly
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


