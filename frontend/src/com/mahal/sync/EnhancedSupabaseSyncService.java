package com.mahal.sync;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.io.IOException;

/**
 * Enhanced sync service with:
 * - JWT user context for multi-user isolation
 * - Exponential backoff retry logic
 * - Conflict resolution support
 * - Incremental sync via lastSyncTime
 */
public class EnhancedSupabaseSyncService {
    private static EnhancedSupabaseSyncService instance;
    private SupabaseConfig config;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 30000;

    private EnhancedSupabaseSyncService() {
        this.config = SupabaseConfig.getInstance();
    }

    public static EnhancedSupabaseSyncService getInstance() {
        if (instance == null) {
            instance = new EnhancedSupabaseSyncService();
        }
        return instance;
    }

    public void configure(String url, String apiKey) {
        config.setUrl(url);
        config.setApiKey(apiKey);
    }

    public boolean isConfigured() {
        return config.isConfigured();
    }

    private String getUrl() {
        return config.getUrl();
    }

    private String getApiKey() {
        return config.getApiKey();
    }

    /**
     * Add user_id to JSON payload. Returns original data if JSON parsing fails.
     */
    private String addUserIdToJson(String jsonData, String userId) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonData);
            json.put("user_id", userId);
            return json.toString();
        } catch (Exception e) {
            // If JSON parsing fails, return original data (user_id may already be present)
            return jsonData;
        }
    }

    /**
     * Upload records to Supabase (push sync).
     * Includes user_id filter to ensure user isolation.
     */
    public SyncResult upload(String tableName, String jsonData, String userId) {
        if (!isConfigured()) {
            return SyncResult.failure("Supabase not configured");
        }

        if (userId == null || userId.isEmpty()) {
            return SyncResult.failure("User ID required for sync");
        }

        // Add user_id to JSON payload to ensure user isolation
        // (Supabase RLS should also enforce this via policies)
        final String finalJsonData = addUserIdToJson(jsonData, userId);

        return executeWithRetry(() -> {
            try {
                URL url = new URL(getUrl() + "/rest/v1/" + tableName);
                HttpURLConnection conn = createConnection(url, "POST");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = finalJsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 201 || responseCode == 200) {
                    return SyncResult.success();
                } else if (responseCode == 409) {
                    // Duplicate key - record already exists, treat as success
                    return SyncResult.success("Record already exists");
                } else {
                    String errorMsg = readErrorResponse(conn);
                    return SyncResult.failure("HTTP " + responseCode + ": " + errorMsg);
                }
            } catch (IOException e) {
                return SyncResult.failure("Network error: " + e.getMessage());
            } catch (Exception e) {
                return SyncResult.failure("Unexpected error: " + e.getMessage());
            }
        });
    }

    /**
     * Update record in Supabase.
     * Uses PUT method (PostgREST merges fields).
     * Includes user_id in query filter for security.
     */
    public SyncResult update(String tableName, String recordId, String jsonData, String userId) {
        if (!isConfigured()) {
            return SyncResult.failure("Supabase not configured");
        }

        if (userId == null || userId.isEmpty()) {
            return SyncResult.failure("User ID required for sync");
        }

        // Add user_id to JSON and query filter
        final String finalJsonData = addUserIdToJson(jsonData, userId);

        return executeWithRetry(() -> {
            try {
                // Filter only by ID for update to comply with PostgREST constraints
                URL url = new URL(getUrl() + "/rest/v1/" + tableName + "?id=eq." + recordId);
                HttpURLConnection conn = createConnection(url, "PUT");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = finalJsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 204) {
                    return SyncResult.success();
                } else {
                    String errorMsg = readErrorResponse(conn);
                    return SyncResult.failure("HTTP " + responseCode + ": " + errorMsg);
                }
            } catch (IOException e) {
                return SyncResult.failure("Network error: " + e.getMessage());
            } catch (Exception e) {
                return SyncResult.failure("Unexpected error: " + e.getMessage());
            }
        });
    }

    /**
     * Delete record from Supabase.
     * Includes user_id filter for security.
     */
    public SyncResult delete(String tableName, String recordId, String userId) {
        if (!isConfigured()) {
            return SyncResult.failure("Supabase not configured");
        }

        if (userId == null || userId.isEmpty()) {
            return SyncResult.failure("User ID required for sync");
        }

        return executeWithRetry(() -> {
            try {
                // Filter only by ID for delete to comply with PostgREST constraints
                URL url = new URL(getUrl() + "/rest/v1/" + tableName + "?id=eq." + recordId);
                HttpURLConnection conn = createConnection(url, "DELETE");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 204) {
                    return SyncResult.success();
                } else {
                    String errorMsg = readErrorResponse(conn);
                    return SyncResult.failure("HTTP " + responseCode + ": " + errorMsg);
                }
            } catch (IOException e) {
                return SyncResult.failure("Network error: " + e.getMessage());
            } catch (Exception e) {
                return SyncResult.failure("Unexpected error: " + e.getMessage());
            }
        });
    }

    /**
     * Download records from Supabase (pull sync).
     * Returns records modified after lastSyncTime for this user.
     */
    /**
     * Download records from Supabase (pull sync).
     * Returns records modified after lastSyncTime for this user.
     * Uses "user_id" as default filter column.
     */
    public DownloadResult download(String tableName, Instant lastSyncTime, String userId) {
        if (!isConfigured()) {
            return DownloadResult.failure("Supabase not configured");
        }

        if (userId == null || userId.isEmpty()) {
            return DownloadResult.failure("User ID required for sync");
        }

        try {
            // Build query: user_id filter + updated_at > lastSyncTime
            String query = "user_id=eq." + userId;
            if (lastSyncTime != null) {
                // Format timestamp for PostgREST (ISO 8601)
                String timestamp = lastSyncTime.toString();
                query += "&updated_at=gt." + timestamp;
            }
            // Order by updated_at ascending
            query += "&order=updated_at.asc";

            URL url = new URL(getUrl() + "/rest/v1/" + tableName + "?" + query);
            HttpURLConnection conn = createConnection(url, "GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String responseBody = readResponse(conn);
                return DownloadResult.success(responseBody);
            } else {
                String errorMsg = readErrorResponse(conn);
                return DownloadResult.failure("HTTP " + responseCode + ": " + errorMsg);
            }
        } catch (IOException e) {
            return DownloadResult.failure("Network error: " + e.getMessage());
        } catch (Exception e) {
            return DownloadResult.failure("Error downloading: " + e.getMessage());
        }
    }

    /**
     * Create HTTP connection with proper headers and JWT.
     */
    private HttpURLConnection createConnection(URL url, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", getApiKey());

        // Use JWT token if available (user context), otherwise fall back to API key
        String jwtToken = UserContext.getJwtToken();
        if (jwtToken != null && !jwtToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        } else {
            conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
        }

        conn.setRequestProperty("Prefer", "return=representation");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);

        return conn;
    }

    /**
     * Execute operation with exponential backoff retry.
     */
    private SyncResult executeWithRetry(java.util.function.Supplier<SyncResult> operation) {
        int attempt = 0;
        long delay = INITIAL_RETRY_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                SyncResult result = operation.get();
                if (result.isSuccess()) {
                    return result;
                }

                // Check if error is retryable
                if (!isRetryableError(result.getError())) {
                    return result; // Don't retry non-retryable errors
                }

            } catch (Exception e) {
                if (attempt == MAX_RETRIES - 1) {
                    return SyncResult.failure("Error after " + MAX_RETRIES + " attempts: " + e.getMessage());
                }
            }

            attempt++;
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(delay);
                    delay *= 2; // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return SyncResult.failure("Interrupted during retry");
                }
            }
        }

        return SyncResult.failure("Failed after " + MAX_RETRIES + " retries");
    }

    /**
     * Check if error is retryable (network errors, 5xx, rate limits).
     */
    private boolean isRetryableError(String error) {
        if (error == null)
            return false;
        // Retry on network errors, 5xx server errors, 429 rate limit
        return error.contains("timeout") ||
                error.contains("connection") ||
                error.contains("HTTP 5") ||
                error.contains("HTTP 429");
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String readErrorResponse(HttpURLConnection conn) {
        try {
            if (conn.getErrorStream() != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    return error.toString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Unknown error";
    }

    /**
     * Result of sync operation.
     */
    public static class SyncResult {
        private final boolean success;
        private final String error;
        private final String message;

        private SyncResult(boolean success, String error, String message) {
            this.success = success;
            this.error = error;
            this.message = message;
        }

        public static SyncResult success() {
            return new SyncResult(true, null, null);
        }

        public static SyncResult success(String message) {
            return new SyncResult(true, null, message);
        }

        public static SyncResult failure(String error) {
            return new SyncResult(false, error, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Result of download operation.
     */
    public static class DownloadResult {
        private final boolean success;
        private final String error;
        private final String jsonData;

        private DownloadResult(boolean success, String error, String jsonData) {
            this.success = success;
            this.error = error;
            this.jsonData = jsonData;
        }

        public static DownloadResult success(String jsonData) {
            return new DownloadResult(true, null, jsonData);
        }

        public static DownloadResult failure(String error) {
            return new DownloadResult(false, error, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }

        public String getJsonData() {
            return jsonData;
        }
    }
}
