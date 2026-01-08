package com.mahal.sync;

import java.util.UUID;

/**
 * Utility for generating and managing UUIDs for distributed sync.
 * Ensures unique IDs across all clients.
 */
public class UuidUtil {
    
    /**
     * Generate a new UUID v4 (random).
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Validate UUID format.
     */
    public static boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Convert UUID to SQLite-friendly format (remove hyphens for storage optimization).
     * Use this if you want to store UUIDs as TEXT without hyphens.
     */
    public static String toCompactUuid(String uuid) {
        if (uuid == null) return null;
        return uuid.replace("-", "");
    }
    
    /**
     * Convert compact UUID back to standard format.
     */
    public static String fromCompactUuid(String compactUuid) {
        if (compactUuid == null || compactUuid.length() != 32) {
            return compactUuid;
        }
        // Insert hyphens: 8-4-4-4-12
        return compactUuid.substring(0, 8) + "-" +
               compactUuid.substring(8, 12) + "-" +
               compactUuid.substring(12, 16) + "-" +
               compactUuid.substring(16, 20) + "-" +
               compactUuid.substring(20, 32);
    }
}
