package com.mahal.util;

import com.mahal.database.DataMigrationHelper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.io.FileInputStream;

/**
 * Helper class to migrate existing data in Supabase to assign user_id to existing records.
 * This updates the Supabase cloud database similar to the local migration.
 */
public class SupabaseDataMigrationHelper {
    private String supabaseUrl;
    private String supabaseKey;
    
    public SupabaseDataMigrationHelper() {
        loadSupabaseConfig();
    }
    
    private void loadSupabaseConfig() {
        try {
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream("supabase.properties");
            props.load(fis);
            fis.close();
            
            // Get URL and remove escape characters if present
            String url = props.getProperty("supabase.url");
            if (url != null) {
                url = url.replace("\\:", ":");
                // Remove https:// prefix if present, we'll add it back
                if (url.startsWith("https://")) {
                    this.supabaseUrl = "https://" + url.replace("https://", "").split("/")[0];
                } else {
                    this.supabaseUrl = "https://" + url.split("/")[0];
                }
            }
            this.supabaseKey = props.getProperty("supabase.key");
        } catch (Exception e) {
            System.err.println("Error loading Supabase config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Assign all existing records in Supabase (without user_id) to a specific user.
     * WARNING: This uses PATCH requests which may not work for bulk updates.
     * For Supabase, you should use SQL directly in the Supabase dashboard or REST API.
     * 
     * @param userId The admin user ID to assign records to
     * @return Number of tables processed
     */
    public int assignExistingDataToUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        System.out.println("\n⚠️  WARNING: Bulk updates via REST API are not recommended.");
        System.out.println("For Supabase, it's better to run SQL directly in the Supabase SQL Editor.");
        System.out.println("\nPlease use the SQL script: sql/assign_existing_data_to_user_supabase.sql");
        System.out.println("Or run the SQL commands directly in Supabase Dashboard > SQL Editor\n");
        
        return 0;
    }
    
    /**
     * Get Supabase REST API URL for a table
     */
    private String getTableUrl(String tableName) {
        return supabaseUrl + "/rest/v1/" + tableName;
    }
}



