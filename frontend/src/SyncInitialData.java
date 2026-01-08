// Utility to perform initial sync of all existing data
// Run: java -cp "bin;lib/sqlite-jdbc.jar" SyncInitialData

import com.mahal.sync.SyncHelper;
import com.mahal.sync.SupabaseConfig;

public class SyncInitialData {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Initial Sync - Syncing All Existing Data");
        System.out.println("========================================");
        
        try {
            // Load configuration
            SupabaseConfig config = SupabaseConfig.getInstance();
            if (!config.isConfigured()) {
                System.err.println("✗ Error: Supabase not configured!");
                System.err.println("Please configure Supabase URL and API key in supabase.properties");
                return;
            }
            
            System.out.println("✓ Supabase configured: " + config.getUrl());
            System.out.println("Starting initial sync of all existing data...");
            System.out.println("This will queue all existing records for sync to Supabase.");
            System.out.println("");
            
            SyncHelper.configureSupabase(config.getUrl(), config.getApiKey());
            SyncHelper.performInitialSync();
            
            System.out.println("");
            System.out.println("✓ Initial sync started!");
            System.out.println("Check console output for progress.");
            System.out.println("Data will sync automatically when internet is available.");
        } catch (Exception e) {
            System.err.println("✗ Error during initial sync: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Keep process alive briefly
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


