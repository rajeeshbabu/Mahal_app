package com.mahal.database;

import com.mahal.model.User;
import com.mahal.sync.SyncHelper;
import com.mahal.sync.SupabaseConfig;
import com.mahal.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;
import org.json.JSONObject;
import org.json.JSONArray;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AdminDAO {
    private DatabaseService dbService;

    public AdminDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS admins (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "full_name TEXT, " +
                "active INTEGER DEFAULT 1, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "admins");
            // Only create indexes if user_id column exists
            try {
                // Check if user_id column exists before creating indexes
                String checkSql = "PRAGMA table_info(admins)";
                var columns = dbService.executeQuery(checkSql, rs -> {
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
                if (hasUserId) {
                    dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admins_user_id ON admins(user_id)", null);
                    dbService.executeUpdate(
                            "CREATE UNIQUE INDEX IF NOT EXISTS idx_admins_user_name_unique ON admins(user_id, name)",
                            null);
                }
            } catch (Exception e) {
                // Index creation failed, but table is created - that's okay
                System.err.println(
                        "Note: Could not create indexes (user_id column may not exist yet): " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error creating admins table: " + e.getMessage());
        }
    }

    public User authenticate(String email, String password) {
        // First, try to find in SQLite
        String sql = "SELECT id, user_id, name, password, full_name, active FROM admins WHERE name = ? AND active = 1";

        try (Connection conn = dbService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Found in SQLite, verify password
                    String hashedPassword = rs.getString("password");
                    if (BCrypt.checkpw(password, hashedPassword)) {
                        User user = new User();
                        user.setId(rs.getLong("id"));
                        user.setEmail(rs.getString("name")); // 'name' column stores email
                        user.setFullName(rs.getString("full_name"));
                        user.setRole("ADMIN");
                        return user;
                    }
                    // Password doesn't match
                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user from SQLite: " + e.getMessage());
            e.printStackTrace();
        }

        // Not found in SQLite, check Supabase
        System.out.println("Admin not found in SQLite, checking Supabase for: " + email);
        JSONObject adminFromSupabase = fetchAdminFromSupabase(email);

        if (adminFromSupabase != null) {
            try {
                // Verify password using hash from Supabase
                String hashedPassword = adminFromSupabase.getString("password");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    // Password matches, sync to SQLite
                    String updatedAt = adminFromSupabase.optString("updated_at", java.time.Instant.now().toString());
                    boolean synced = upsertFromSupabase(adminFromSupabase, updatedAt);

                    if (synced) {
                        System.out.println("Successfully synced admin from Supabase to SQLite: " + email);
                        // Now retrieve from SQLite using the ID from Supabase record
                        Long adminId = adminFromSupabase.getLong("id");
                        User user = getById(adminId);
                        if (user != null) {
                            user.setRole("ADMIN");
                            return user;
                        } else {
                            System.err.println("Admin synced but could not retrieve from SQLite: " + email);
                        }
                    } else {
                        System.err.println("Failed to sync admin from Supabase to SQLite: " + email);
                    }
                } else {
                    System.out.println("Password mismatch for admin from Supabase: " + email);
                }
            } catch (Exception e) {
                System.err.println("Error processing admin from Supabase: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return null; // User not found or password doesn't match
    }

    /**
     * Fetch admin record from Supabase by email (name).
     * Returns JSONObject with admin data, or null if not found.
     */
    private JSONObject fetchAdminFromSupabase(String email) {
        SupabaseConfig config = SupabaseConfig.getInstance();
        if (!config.isConfigured()) {
            System.out.println("Supabase not configured, cannot fetch admin from Supabase");
            return null;
        }

        try {
            String urlStr = config.getUrl() + "/rest/v1/admins?name=eq." + java.net.URLEncoder.encode(email, "UTF-8")
                    + "&active=eq.1&limit=1";
            System.out.println("Connecting to Supabase URL: " + urlStr);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("apikey", config.getApiKey());
            conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                String responseBody = response.toString();
                JSONArray records = new JSONArray(responseBody);

                if (records.length() > 0) {
                    JSONObject admin = records.getJSONObject(0);
                    System.out.println("Found admin in Supabase: " + email);
                    return admin;
                } else {
                    System.out.println("Admin not found in Supabase: " + email);
                    return null;
                }
            } else {
                System.err.println("Failed to fetch admin from Supabase. HTTP " + responseCode);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error fetching admin from Supabase: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public User findByEmail(String email) {
        String sql = "SELECT id, user_id, name, full_name, active FROM admins WHERE name = ?";
        List<User> results = dbService.executeQuery(sql, new Object[] { email }, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    public Long create(String email, String password, String fullName) {
        // Hash the password using BCrypt before storing
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        // Insert with temporary user_id, then update it with the generated id
        String sql = "INSERT INTO admins (user_id, name, password, full_name, active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 1, datetime('now'), datetime('now'))";
        Object[] params = { "0", email, hashedPassword, fullName }; // Temporary user_id
        Long id = dbService.executeInsert(sql, params);
        // Update user_id with the generated id (admin id = user_id for admins)
        if (id != null) {
            try {
                dbService.executeUpdate("UPDATE admins SET user_id = ? WHERE id = ?",
                        new Object[] { String.valueOf(id), id });
                // Queue for sync to Supabase with complete admin data
                JSONObject adminJson = getAdminAsJson(id);
                if (adminJson != null) {
                    SyncHelper.queueInsert("admins", id, adminJson);
                }
            } catch (Exception e) {
                System.err.println("Error updating user_id: " + e.getMessage());
            }
        }
        return id;
    }

    public User getById(Long id) {
        String sql = "SELECT id, user_id, name, full_name, active FROM admins WHERE id = ?";
        List<User> results = dbService.executeQuery(sql, new Object[] { id }, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get complete admin record as JSONObject for syncing (includes all fields).
     */
    private JSONObject getAdminAsJson(Long id) {
        String sql = "SELECT id, user_id, name, password, full_name, active, created_at, updated_at FROM admins WHERE id = ?";
        try (Connection conn = dbService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    JSONObject adminJson = new JSONObject();
                    adminJson.put("id", rs.getLong("id"));
                    adminJson.put("user_id", rs.getString("user_id"));
                    adminJson.put("name", rs.getString("name"));
                    adminJson.put("password", rs.getString("password")); // BCrypt hash
                    adminJson.put("full_name", rs.getString("full_name"));
                    adminJson.put("active", rs.getInt("active"));
                    adminJson.put("created_at", rs.getString("created_at"));
                    adminJson.put("updated_at", rs.getString("updated_at"));
                    return adminJson;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting admin as JSON: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean update(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        String sql = "UPDATE admins SET name = ?, full_name = ?, updated_at = datetime('now') WHERE id = ?";
        Object[] params = { user.getEmail(), user.getFullName(), user.getId() };
        boolean success = dbService.executeUpdate(sql, params) > 0;
        // Queue for sync if update was successful
        if (success) {
            JSONObject adminJson = getAdminAsJson(user.getId());
            if (adminJson != null) {
                SyncHelper.queueUpdate("admins", user.getId(), adminJson);
            }
        }
        return success;
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM admins WHERE id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id }) > 0;
        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("admins", id);
        }
        return success;
    }

    /**
     * Upsert a record from Supabase (insert if new, update if exists).
     * Does NOT queue for sync (to avoid sync loops).
     * Used for bidirectional sync (Supabase → Local).
     */
    public boolean upsertFromSupabase(JSONObject record, String supabaseUpdatedAt) {
        try {
            Long id = record.getLong("id");
            String userId = record.optString("user_id", String.valueOf(id));
            String name = record.getString("name");
            String password = record.getString("password"); // BCrypt hash from Supabase
            String fullName = record.optString("full_name", null);
            int active = record.optInt("active", 1);

            // Check if record exists locally
            User existing = getById(id);

            if (existing != null) {
                // Update existing record
                String sql = "UPDATE admins SET name = ?, password = ?, full_name = ?, active = ?, user_id = ?, updated_at = datetime('now') WHERE id = ?";
                Object[] params = { name, password, fullName, active, userId, id };
                boolean success = dbService.executeUpdate(sql, params) > 0;
                if (success) {
                    System.out.println("AdminDAO.upsertFromSupabase: Updated admin ID " + id + " from Supabase");
                }
                return success;
            } else {
                // Insert new record with explicit ID
                String sql = "INSERT INTO admins (id, user_id, name, password, full_name, active, created_at, updated_at) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
                Object[] params = { id, userId, name, password, fullName, active };
                // Use executeUpdate when inserting with explicit ID (like StaffDAO)
                int rows = dbService.executeUpdate(sql, params);
                if (rows > 0) {
                    System.out.println("AdminDAO.upsertFromSupabase: Inserted admin ID " + id + " from Supabase");
                }
                return rows > 0;
            }
        } catch (Exception e) {
            System.err.println("AdminDAO.upsertFromSupabase error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get current user ID from session.
     */
    private String getCurrentUserId() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            // For authentication or initial setup, we might not have a session yet.
            // But getAll() and getAllAsJson() are usually called post-login.
            return null;
        }
        return String.valueOf(currentUser.getId());
    }

    public List<User> getAll() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new java.util.ArrayList<>();
        }
        // Filter by user_id to show only the current admin
        String sql = "SELECT id, user_id, name, full_name, active FROM admins WHERE user_id = ? ORDER BY id";
        return dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
    }

    /**
     * Get all admins as JSONObjects for initial sync (includes all fields).
     * This method is used for syncing complete admin records to Supabase.
     */
    public List<JSONObject> getAllAsJson() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new java.util.ArrayList<>();
        }
        // Filter by user_id to sync only the current admin
        String sql = "SELECT id, user_id, name, password, full_name, active, created_at, updated_at FROM admins WHERE user_id = ? ORDER BY id";
        List<JSONObject> results = new java.util.ArrayList<>();
        try (Connection conn = dbService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject adminJson = new JSONObject();
                    adminJson.put("id", rs.getLong("id"));
                    adminJson.put("user_id", rs.getString("user_id"));
                    adminJson.put("name", rs.getString("name"));
                    adminJson.put("password", rs.getString("password")); // BCrypt hash
                    adminJson.put("full_name", rs.getString("full_name"));
                    adminJson.put("active", rs.getInt("active"));
                    adminJson.put("created_at", rs.getString("created_at"));
                    adminJson.put("updated_at", rs.getString("updated_at"));
                    results.add(adminJson);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting all admins as JSON: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    private User mapResultSet(ResultSet rs) {
        try {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("name")); // 'name' column stores email
            user.setFullName(rs.getString("full_name"));
            user.setRole("ADMIN"); // Default role
            return user;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet: " + e.getMessage());
            return null;
        }
    }
}
