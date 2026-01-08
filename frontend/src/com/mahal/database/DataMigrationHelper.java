package com.mahal.database;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to migrate existing data to assign user_id to existing records.
 * This is needed for data that was created before user_id filtering was implemented.
 */
public class DataMigrationHelper {
    private DatabaseService dbService;
    
    public DataMigrationHelper() {
        this.dbService = DatabaseService.getInstance();
    }
    
    /**
     * Assign all existing records (without user_id) to a specific user.
     * @param userId The admin user ID to assign records to
     * @return Number of tables updated
     */
    public int assignExistingDataToUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        int tablesUpdated = 0;
        List<String> tables = getAllTables();
        
        for (String tableName : tables) {
            try {
                // Check if table has user_id column
                if (hasUserIdColumn(tableName)) {
                    int rowsAffected = updateUserIdForTable(tableName, userId);
                    if (rowsAffected > 0) {
                        System.out.println("Updated " + rowsAffected + " records in " + tableName + " to user_id = " + userId);
                        tablesUpdated++;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating " + tableName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return tablesUpdated;
    }
    
    /**
     * Get list of all tables that should have user_id
     */
    private List<String> getAllTables() {
        List<String> tables = new ArrayList<>();
        tables.add("members");
        tables.add("incomes");
        tables.add("expenses");
        tables.add("due_collections");
        tables.add("masjids");
        tables.add("committees");
        tables.add("staff");
        tables.add("staff_salaries");
        tables.add("income_types");
        tables.add("due_types");
        tables.add("events");
        tables.add("inventory_items");
        tables.add("houses");
        tables.add("damaged_items");
        tables.add("rent_items");
        tables.add("rents");
        tables.add("marriage_certificates");
        tables.add("death_certificates");
        tables.add("jamath_certificates");
        tables.add("custom_certificates");
        return tables;
    }
    
    /**
     * Check if table has user_id column
     */
    private boolean hasUserIdColumn(String tableName) {
        try {
            String sql = "PRAGMA table_info(" + tableName + ")";
            var columns = dbService.executeQuery(sql, rs -> {
                try {
                    return rs.getString("name");
                } catch (Exception e) {
                    return null;
                }
            });
            
            for (var col : columns) {
                if ("user_id".equals(col)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Update user_id for records that don't have it set
     */
    private int updateUserIdForTable(String tableName, String userId) {
        try {
            // First, try to update records where user_id is NULL
            String sql = "UPDATE " + tableName + " SET user_id = ? WHERE user_id IS NULL OR user_id = ''";
            int rowsAffected = dbService.executeUpdate(sql, new Object[]{userId});
            return rowsAffected;
        } catch (Exception e) {
            System.err.println("Error updating " + tableName + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get admin user ID by email/name or full_name
     * @param emailOrName The email, name, or full_name of the admin
     * @return The user ID as String, or null if not found
     */
    public String getAdminIdByName(String emailOrName) {
        try {
            // First try by name (email)
            String sql = "SELECT id FROM admins WHERE name = ? OR full_name = ?";
            var results = dbService.executeQuery(sql, new Object[]{emailOrName, emailOrName}, rs -> {
                try {
                    return String.valueOf(rs.getLong("id"));
                } catch (Exception e) {
                    return null;
                }
            });
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            System.err.println("Error finding admin ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * List all admin users with their IDs
     */
    public void listAllAdmins() {
        try {
            String sql = "SELECT id, name, full_name FROM admins ORDER BY id";
            System.out.println("\n=== Admin Users ===");
            System.out.println("ID\tName\t\tFull Name");
            System.out.println("-----------------------------------");
            var results = dbService.executeQuery(sql, rs -> {
                try {
                    return rs.getLong("id") + "\t" + 
                           rs.getString("name") + "\t\t" + 
                           (rs.getString("full_name") != null ? rs.getString("full_name") : "");
                } catch (Exception e) {
                    return null;
                }
            });
            for (var row : results) {
                System.out.println(row);
            }
            System.out.println("===================================\n");
        } catch (Exception e) {
            System.err.println("Error listing admins: " + e.getMessage());
        }
    }
}



