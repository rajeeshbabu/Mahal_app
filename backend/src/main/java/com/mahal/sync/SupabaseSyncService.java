package com.mahal.sync;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service to sync data with Supabase using REST API.
 * Used by backend Spring Boot application to sync subscriptions and other data.
 */
@Service
public class SupabaseSyncService {

    private static SupabaseSyncService instance;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.key:}")
    private String supabaseApiKey;

    public SupabaseSyncService() {
        instance = this;
    }

    public static SupabaseSyncService getInstance() {
        if (instance == null) {
            instance = new SupabaseSyncService();
        }
        return instance;
    }

    /**
     * Configure Supabase connection.
     */
    public void configure(String url, String apiKey) {
        this.supabaseUrl = url;
        this.supabaseApiKey = apiKey;
    }

    /**
     * Get Supabase URL. Fallback to system properties if not set via Spring.
     */
    public String getSupabaseUrl() {
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            supabaseUrl = System.getProperty("SUPABASE_URL");
            if (supabaseUrl == null || supabaseUrl.isEmpty()) {
                supabaseUrl = System.getenv("SUPABASE_URL");
            }
        }
        return supabaseUrl;
    }

    /**
     * Get Supabase API key. Fallback to system properties if not set via Spring.
     */
    public String getSupabaseApiKey() {
        if (supabaseApiKey == null || supabaseApiKey.isEmpty()) {
            supabaseApiKey = System.getProperty("SUPABASE_KEY");
            if (supabaseApiKey == null || supabaseApiKey.isEmpty()) {
                supabaseApiKey = System.getenv("SUPABASE_KEY");
            }
        }
        return supabaseApiKey;
    }

    /**
     * Check if Supabase is configured.
     */
    public boolean isConfigured() {
        String url = getSupabaseUrl();
        String key = getSupabaseApiKey();
        return url != null && !url.isEmpty() && key != null && !key.isEmpty();
    }

    /**
     * Add user_id to JSON payload to ensure user isolation.
     */
    private String addUserIdToJson(String jsonData, String userId) {
        try {
            String userIdStr = String.valueOf(userId).trim();
            if (userIdStr.isEmpty() || "null".equals(userIdStr)) {
                return jsonData;
            }
            JSONObject json = new JSONObject(jsonData);
            json.put("user_id", userIdStr);
            return json.toString();
        } catch (Exception e) {
            return jsonData;
        }
    }

    /**
     * Insert a record into Supabase.
     */
    public boolean insert(String tableName, String jsonData, String userId) {
        if (!isConfigured())
            return false;

        try {
            String finalJsonData = addUserIdToJson(jsonData, userId);
            String fullUrl = getSupabaseUrl() + "/rest/v1/" + tableName;

            if ("subscriptions".equals(tableName)) {
                fullUrl += "?on_conflict=user_id";
                // Strip ID for subscriptions as user_id is PK
                try {
                    JSONObject json = new JSONObject(finalJsonData);
                    json.remove("id");
                    finalJsonData = json.toString();
                } catch (Exception e) {
                }
            }

            URL url = java.net.URI.create(fullUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("apikey", getSupabaseApiKey());
            conn.setRequestProperty("Authorization", "Bearer " + getSupabaseApiKey());
            conn.setRequestProperty("Prefer", "return=representation,resolution=merge-duplicates");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(finalJsonData.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            return code == 201 || code == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update a record in Supabase.
     */
    public boolean update(String tableName, String recordId, String jsonData, String userId) {
        if (!isConfigured())
            return false;
        try {
            String finalJsonData = addUserIdToJson(jsonData, userId);
            String matchField = "id";
            String matchValue = recordId;

            if ("subscriptions".equals(tableName) && userId != null) {
                matchField = "user_id";
                matchValue = userId;
            }

            String fullUrl = getSupabaseUrl() + "/rest/v1/" + tableName + "?" + matchField + "=eq."
                    + URLEncoder.encode(matchValue, StandardCharsets.UTF_8);

            // Strip ID for updates
            try {
                JSONObject json = new JSONObject(finalJsonData);
                json.remove("id");
                finalJsonData = json.toString();
            } catch (Exception e) {
            }

            URL url = java.net.URI.create(fullUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("apikey", getSupabaseApiKey());
            conn.setRequestProperty("Authorization", "Bearer " + getSupabaseApiKey());
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(finalJsonData.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            return code == 200 || code == 204;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a record from Supabase.
     */
    public boolean delete(String tableName, String recordId, String userId) {
        if (!isConfigured())
            return false;
        try {
            String filter;
            if ("subscriptions".equals(tableName)) {
                filter = userId.contains("@") ? "user_email=eq." + URLEncoder.encode(userId, StandardCharsets.UTF_8)
                        : "user_id=eq." + userId;
            } else {
                filter = "id=eq." + recordId + "&user_id=eq." + userId;
            }

            String fullUrl = getSupabaseUrl() + "/rest/v1/" + tableName + "?" + filter;
            URL url = java.net.URI.create(fullUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("apikey", getSupabaseApiKey());
            conn.setRequestProperty("Authorization", "Bearer " + getSupabaseApiKey());
            return conn.getResponseCode() <= 204;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetch the full subscription record for a user from Supabase.
     */
    public JSONObject fetchSubscription(String userIdentifier) {
        if (!isConfigured() || userIdentifier == null)
            return null;
        try {
            String encodedId = URLEncoder.encode(userIdentifier, StandardCharsets.UTF_8);
            String filter = userIdentifier.contains("@")
                    ? "or=(user_id.eq." + encodedId + ",user_email.eq." + encodedId + ")"
                    : "user_id.eq." + encodedId;
            String fullUrl = getSupabaseUrl() + "/rest/v1/subscriptions?" + filter + "&order=created_at.desc&limit=1";

            URL url = java.net.URI.create(fullUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", getSupabaseApiKey());
            conn.setRequestProperty("Authorization", "Bearer " + getSupabaseApiKey());

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                        sb.append(line);
                    org.json.JSONArray array = new org.json.JSONArray(sb.toString());
                    return array.length() > 0 ? array.getJSONObject(0) : null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching subscription: " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetch the latest subscription ID for a user from Supabase.
     */
    public String fetchLatestSubscriptionId(String userId) {
        JSONObject sub = fetchSubscription(userId);
        return (sub != null && sub.has("id")) ? String.valueOf(sub.get("id")) : null;
    }

    /**
     * Fetch all subscription pricing from Supabase.
     */
    public org.json.JSONArray fetchAllPricing() {
        if (!isConfigured()) {
            System.err.println("✗ Supabase sync failure: SUPABASE_KEY is not set in environment or properties.");
            return null;
        }
        try {
            String fullUrl = getSupabaseUrl() + "/rest/v1/subscription_pricing?order=plan_duration.asc";
            URL url = java.net.URI.create(fullUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", getSupabaseApiKey());
            conn.setRequestProperty("Authorization", "Bearer " + getSupabaseApiKey());

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                        sb.append(line);
                    return new org.json.JSONArray(sb.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching pricing: " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetch records from Supabase with a filter.
     */
    public String fetch(String tableName, String filter) {
        if (!isConfigured())
            return null;
        try {
            String fullUrl = getSupabaseUrl() + "/rest/v1/" + tableName
                    + (filter != null && !filter.isEmpty() ? "?" + filter : "");
            URL url = java.net.URI.create(fullUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", getSupabaseApiKey());
            conn.setRequestProperty("Authorization", "Bearer " + getSupabaseApiKey());

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                        sb.append(line);
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
