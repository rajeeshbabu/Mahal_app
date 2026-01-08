package com.mahal.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.mahal.util.SessionManager;
import org.json.JSONObject;
import org.json.JSONArray;

public class ApiService {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static ApiService instance;

    private ApiService() {
    }

    public static ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    /**
     * Wait for the backend server to be responsive.
     * Useful during startup to avoid race conditions.
     */
    public boolean waitForServer(int timeoutSeconds) {
        System.out.println("⏳ Waiting for backend to start (timeout: " + timeoutSeconds + "s)...");
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000) {
            try {
                // Use a simple endpoint that should be available
                URL url = java.net.URI.create(BASE_URL + "/pricing").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                int code = conn.getResponseCode();
                if (code > 0) { // Any response (even error) means server is listening
                    System.out.println("✅ Backend is responsive!");
                    return true;
                }
            } catch (Exception e) {
                // Server not listening yet
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.err.println("❌ Backend failed to start within " + timeoutSeconds + "s");
        return false;
    }

    public ApiResponse post(String endpoint, JSONObject data) {
        return makeRequest("POST", endpoint, data);
    }

    public ApiResponse get(String endpoint) {
        return makeRequest("GET", endpoint, null);
    }

    public ApiResponse put(String endpoint, JSONObject data) {
        return makeRequest("PUT", endpoint, data);
    }

    public ApiResponse delete(String endpoint) {
        return makeRequest("DELETE", endpoint, null);
    }

    private ApiResponse makeRequest(String method, String endpoint, JSONObject data) {
        try {
            URL url = java.net.URI.create(BASE_URL + endpoint).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");

            // Add authorization token if available
            String token = SessionManager.getInstance().getAuthToken();
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (data != null && (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = data.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();

            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return new ApiResponse(responseCode, response.toString());

        } catch (Exception e) {
            return new ApiResponse(500, "{\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    public static class ApiResponse {
        private int statusCode;
        private String body;

        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public JSONObject getJson() {
            try {
                return new JSONObject(body);
            } catch (Exception e) {
                return new JSONObject();
            }
        }

        public JSONArray getJsonArray() {
            try {
                return new JSONArray(body);
            } catch (Exception e) {
                return new JSONArray();
            }
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
