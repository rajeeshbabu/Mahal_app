package com.mahal.sync;

import org.json.JSONObject;

/**
 * Utility for parsing JWT tokens to extract user_id.
 * In production, use a proper JWT library (e.g., java-jwt or jose4j).
 * This is a minimal implementation for extracting user_id from JWT payload.
 */
public class JwtUtil {
    
    /**
     * Extract user_id from JWT token.
     * Assumes token format: header.payload.signature
     * Payload contains user_id claim.
     * 
     * @param jwtToken The JWT token string
     * @return user_id or null if invalid/not found
     */
    public static String extractUserId(String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            return null;
        }
        
        try {
            // Split JWT: header.payload.signature
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Decode payload (base64url)
            String payload = parts[1];
            // Add padding if needed for base64 decoding
            int padding = (4 - (payload.length() % 4)) % 4;
            for (int i = 0; i < padding; i++) {
                payload += "=";
            }
            
            // Decode base64
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String payloadJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            
            // Parse JSON and extract user_id
            JSONObject json = new JSONObject(payloadJson);
            
            // Try common claim names
            if (json.has("user_id")) {
                return json.getString("user_id");
            }
            if (json.has("sub")) { // Standard JWT subject claim
                return json.getString("sub");
            }
            if (json.has("userId")) {
                return json.getString("userId");
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error extracting user_id from JWT: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate JWT token format (basic check only).
     * In production, verify signature using secret key.
     */
    public static boolean isValidFormat(String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            return false;
        }
        String[] parts = jwtToken.split("\\.");
        return parts.length == 3;
    }
}
