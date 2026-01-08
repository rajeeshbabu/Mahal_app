package com.mahal.service;

import com.mahal.database.DatabaseService;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to handle cleanup of local database data.
 * Ensures that only the currently logged-in user's data is retained on the
 * device.
 */
public class DataCleanupService {
    private static DataCleanupService instance;
    private DatabaseService dbService;

    private DataCleanupService() {
        this.dbService = DatabaseService.getInstance();
    }

    public static DataCleanupService getInstance() {
        if (instance == null) {
            instance = new DataCleanupService();
        }
        return instance;
    }

    /**
     * Retains ONLY the data belonging to the specified user ID.
     * Deletes all records from all user-scoped tables where user_id !=
     * currentUserId.
     * 
     * @param userId The ID of the currently logged-in user.
     */
    public void retainOnlyUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            System.err.println("⚠️ [CLEANUP] Cannot perform cleanup: User ID is null or empty.");
            return;
        }

        System.out.println("🧹 [CLEANUP] Starting global data cleanup for user: " + userId);

        List<String> tables = getAllUserScopedTables();
        int totalDeleted = 0;

        for (String tableName : tables) {
            try {
                if (hasUserIdColumn(tableName)) {
                    String sql = "DELETE FROM " + tableName + " WHERE user_id != ?";
                    int rows = dbService.executeUpdate(sql, new Object[] { userId });
                    if (rows > 0) {
                        System.out.println(
                                "   - Removed " + rows + " records from '" + tableName + "' belonging to other users.");
                        totalDeleted += rows;
                    }
                }
            } catch (Exception e) {
                System.err.println("   - Error cleaning table '" + tableName + "': " + e.getMessage());
            }
        }

        System.out.println("✅ [CLEANUP] Completed. Total records removed: " + totalDeleted);
    }

    /**
     * Returns a list of all tables that contain user-specific data.
     */
    private List<String> getAllUserScopedTables() {
        List<String> tables = new ArrayList<>();
        // Core tables
        // Core tables
        // tables.add("admins"); // DO NOT cleanup admins table. It contains the
        // logged-in user!
        tables.add("members");
        tables.add("masjids");
        tables.add("committees");
        tables.add("staff");

        // Finance
        tables.add("incomes");
        tables.add("expenses");
        tables.add("due_collections");
        tables.add("staff_salaries");

        // Types/Categories
        tables.add("income_types");
        tables.add("due_types");

        // Inventory
        tables.add("inventory_items");
        tables.add("damaged_items");
        tables.add("rent_items"); // If exists

        // Other modules
        tables.add("events");
        tables.add("houses");
        tables.add("rents");

        // Certificates
        tables.add("marriage_certificates");
        tables.add("death_certificates");
        tables.add("jamath_certificates");
        tables.add("custom_certificates");

        return tables;
    }

    /**
     * Verifies if a table actually has a user_id column before trying to delete
     * from it.
     * This prevents SQL errors if schema changes.
     */
    private boolean hasUserIdColumn(String tableName) {
        try {
            String sql = "PRAGMA table_info(" + tableName + ")";
            // We use a simple list check here
            List<String> columns = dbService.executeQuery(sql, rs -> {
                try {
                    return rs.getString("name");
                } catch (Exception e) {
                    return null;
                }
            });

            return columns != null && columns.contains("user_id");
        } catch (Exception e) {
            System.err.println("Error checking schema for " + tableName + ": " + e.getMessage());
            return false;
        }
    }
}
