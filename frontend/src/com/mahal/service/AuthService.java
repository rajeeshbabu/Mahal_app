package com.mahal.service;

import com.mahal.model.User;
import com.mahal.util.SessionManager;
import com.mahal.database.AdminDAO;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class AuthService {
    private AdminDAO adminDAO;

    public AuthService() {
        this.adminDAO = new AdminDAO();
    }

    public boolean login(String email, String password) {
        // Note: In production, passwords should be hashed.
        // For now, we'll do a simple comparison (assuming passwords are stored as plain
        // text or hashed in DB)
        User user = adminDAO.authenticate(email, password);

        if (user != null) {
            // Generate a simple token (in production, use JWT)
            String token = generateSimpleToken(email);
            SessionManager.getInstance().setUser(user, token);

            // CLEANUP: Remove ANY data belonging to other users (admins, members, incomes,
            // etc)
            // This ensures complete local database isolation
            if (user.getId() != null) {
                DataCleanupService.getInstance().retainOnlyUser(String.valueOf(user.getId()));
            }

            return true;
        }
        return false;
    }

    public Long register(String email, String password, String fullName) {
        // Check if user already exists
        User existing = adminDAO.findByEmail(email);
        if (existing != null) {
            return null; // User already exists
        }

        // Create new user (password is hashed in AdminDAO)
        Long adminId = adminDAO.create(email, password, fullName);
        if (adminId != null) {
            try {
                // Queue creation of initial subscription record
                // This ensures the backend sees the user in the subscriptions table
                org.json.JSONObject subJson = new org.json.JSONObject();
                subJson.put("user_id", String.valueOf(adminId));
                subJson.put("user_email", email);
                subJson.put("plan_duration", "monthly"); // Default to monthly pending
                subJson.put("status", "pending");
                subJson.put("superadmin_status", "activated");
                String now = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
                subJson.put("created_at", now);
                subJson.put("updated_at", now);

                // Queue the insert. Use adminId as recordId for queue tracking
                com.mahal.sync.SyncHelper.queueInsert("subscriptions", adminId, subJson);
                System.out.println("Queued initial subscription creation for admin: " + email);
            } catch (Exception e) {
                System.err.println("Error queuing initial subscription: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return adminId;
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }

    private String generateSimpleToken(String email) {
        // Simple token generation (in production, use proper JWT)
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((email + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return email + "_" + System.currentTimeMillis();
        }
    }
}
