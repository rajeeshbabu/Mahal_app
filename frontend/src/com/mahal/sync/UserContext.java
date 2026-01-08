package com.mahal.sync;

/**
 * Thread-local user context for sync operations.
 * Extracted from JWT token on each request.
 */
public class UserContext {
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> jwtTokenHolder = new ThreadLocal<>();
    
    /**
     * Set current user ID (from JWT token claims).
     */
    public static void setUserId(String userId) {
        userIdHolder.set(userId);
    }
    
    /**
     * Get current user ID.
     */
    public static String getUserId() {
        return userIdHolder.get();
    }
    
    /**
     * Set current JWT token.
     */
    public static void setJwtToken(String token) {
        jwtTokenHolder.set(token);
    }
    
    /**
     * Get current JWT token.
     */
    public static String getJwtToken() {
        return jwtTokenHolder.get();
    }
    
    /**
     * Clear thread-local context (call after request completion).
     */
    public static void clear() {
        userIdHolder.remove();
        jwtTokenHolder.remove();
    }
    
    /**
     * Check if user context is set.
     */
    public static boolean hasUserId() {
        return userIdHolder.get() != null;
    }
}
