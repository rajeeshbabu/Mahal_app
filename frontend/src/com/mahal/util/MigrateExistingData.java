package com.mahal.util;

import com.mahal.database.DataMigrationHelper;

/**
 * Utility class to run data migration for existing records.
 * This assigns all existing records to a specific user.
 * 
 * Usage:
 * 1. Run this class to see all admin users
 * 2. Choose which user ID to assign existing data to
 * 3. Run the migration
 */
public class MigrateExistingData {
    
    public static void main(String[] args) {
        DataMigrationHelper helper = new DataMigrationHelper();
        
        // Step 1: List all admin users
        System.out.println("Listing all admin users...");
        helper.listAllAdmins();
        
        // Step 2: Get user ID - change "user1" to the admin name you want to assign data to
        // If you want to assign to "sam", change to "sam"
        // If you want to assign to "user1", keep as "user1"
        String adminName = args.length > 0 ? args[0] : "user1"; // Default to "user1", or use command line argument
        String userId = helper.getAdminIdByName(adminName);
        
        if (userId == null) {
            System.err.println("Admin user '" + adminName + "' not found!");
            System.out.println("\nPlease check the admin list above and use a valid admin name.");
            System.out.println("\nUsage: java MigrateExistingData <admin_name>");
            System.out.println("Example: java MigrateExistingData user1");
            System.out.println("Example: java MigrateExistingData sam");
            return;
        }
        
        System.out.println("Found admin '" + adminName + "' with ID: " + userId);
        System.out.println("\nStarting migration...");
        System.out.println("Assigning all existing records (without user_id) to user_id = " + userId);
        System.out.println("This will make all existing data visible to: " + adminName);
        
        // Step 3: Assign existing data to this user
        int tablesUpdated = helper.assignExistingDataToUser(userId);
        
        System.out.println("\n========================================");
        System.out.println("Migration completed!");
        System.out.println("Updated " + tablesUpdated + " tables.");
        System.out.println("All existing records are now assigned to user: " + adminName + " (ID: " + userId + ")");
        System.out.println("You can now log in as '" + adminName + "' to see all the existing data.");
        System.out.println("========================================");
    }
}
