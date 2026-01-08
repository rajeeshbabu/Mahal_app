package com.mahal.database;

import com.mahal.util.SessionManager;
import com.mahal.model.User;

/**
 * Base utility methods for DAOs to ensure user isolation.
 */
public class DAOBase {

    /**
     * Get current user ID from session.
     * Throws IllegalStateException if no user is logged in.
     */
    protected static String getCurrentUserId() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException(
                    "No user logged in. Cannot perform database operation without user context.");
        }
        return String.valueOf(currentUser.getId());
    }

    /**
     * Get current user ID, or return null if no user logged in (non-throwing
     * version).
     */
    protected static String getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Ensure table has user_id column (for migration).
     * Call this in createTableIfNotExists() after creating the table.
     */
    protected static void ensureUserIdColumn(DatabaseService db, String tableName) {
        try {
            // Check if user_id column exists
            String checkSql = "PRAGMA table_info(" + tableName + ")";
            var columns = db.executeQuery(checkSql, rs -> {
                try {
                    return rs.getString("name");
                } catch (Exception e) {
                    return null;
                }
            });

            boolean hasUserId = false;
            for (var col : columns) {
                if ("user_id".equals(col)) {
                    hasUserId = true;
                    break;
                }
            }

            if (!hasUserId) {
                // Add user_id column
                String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN user_id TEXT";
                db.executeUpdate(alterSql, null);
                System.out.println("Added user_id column to " + tableName);
            }
        } catch (Exception e) {
            // Table might not exist yet or column might already exist, ignore
            System.err.println("Note: Could not check/add user_id column to " + tableName + ": " + e.getMessage());
        }
    }
}
