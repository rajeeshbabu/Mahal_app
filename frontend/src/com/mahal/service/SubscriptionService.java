package com.mahal.service;

import com.mahal.service.ApiService.ApiResponse;
import com.mahal.util.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to check subscription status and create subscriptions
 */
public class SubscriptionService {
    private static SubscriptionService instance;
    private ApiService apiService;

    private SubscriptionService() {
        this.apiService = ApiService.getInstance();
    }

    public static SubscriptionService getInstance() {
        if (instance == null) {
            instance = new SubscriptionService();
        }
        return instance;
    }

    /**
     * Check if user has an active subscription
     * Returns null if backend is unreachable (app should be locked)
     */
    public SubscriptionStatus checkSubscriptionStatus() {
        try {
            // Get logged-in user's email to check their specific subscription
            String userEmail = getCurrentUserEmail();
            Long userId = getCurrentUserId();
            String endpoint = "/subscriptions/status";
            if (userId != null) {
                endpoint += "?userId=" + userId;
                if (userEmail != null && !userEmail.isEmpty()) {
                    endpoint += "&email=" + java.net.URLEncoder.encode(userEmail, "UTF-8");
                }
            } else if (userEmail != null && !userEmail.isEmpty()) {
                endpoint += "?userId=" + java.net.URLEncoder.encode(userEmail, "UTF-8") +
                        "&email=" + java.net.URLEncoder.encode(userEmail, "UTF-8");
            }

            ApiResponse response = apiService.get(endpoint);

            if (response.getStatusCode() == 401 || response.getStatusCode() == 403) {
                // User not authenticated
                return new SubscriptionStatus(false, "unauthorized", null, null);
            }

            if (!response.isSuccess()) {
                // Backend error - treat as inactive
                return new SubscriptionStatus(false, "error", null, null);
            }

            JSONObject json = response.getJson();
            System.out.println("🔍 [DEBUG] Subscription JSON keys: " + json.keySet());
            System.out.println("🔍 [DEBUG] Subscription JSON content: " + json.toString());

            boolean active = json.optBoolean("active", false)
                    || "active".equalsIgnoreCase(json.optString("status", ""));
            String status = json.optString("status", "not_found");
            String planDuration = json.optString("planDuration", null);

            LocalDateTime endDate = null;
            if (json.has("endDate") && !json.isNull("endDate")) {
                String endDateStr = json.getString("endDate");
                try {
                    endDate = LocalDateTime.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }

            return new SubscriptionStatus(active, status, planDuration, endDate);
        } catch (Exception e) {
            // Network error or backend unreachable - treat as inactive
            System.err.println("Error checking subscription status: " + e.getMessage());
            return new SubscriptionStatus(false, "error", null, null);
        }
    }

    /**
     * Create a subscription (Monthly or Yearly)
     * Returns the Razorpay checkout URL
     */
    public String createSubscription(String planDuration) {
        try {
            // 1. Get current user info
            Long userId = getCurrentUserId();
            if (userId == null) {
                throw new RuntimeException("No user logged in. Please log in to subscribe.");
            }

            // 2. Fetch price needed for the Edge Function
            Map<String, String> pricing = getPricing();
            String priceStr = pricing.get(planDuration.toLowerCase());
            if (priceStr == null) {
                // Try fallback if map key is different
                priceStr = pricing.get(planDuration);
            }

            // Extract numeric amount from "₹500" format
            String amountRupees = "1"; // Default fallback
            if (priceStr != null) {
                amountRupees = priceStr.replaceAll("[^0-9]", "");
            }

            // 3. Call the secure Supabase Edge Function
            JSONObject request = new JSONObject();
            request.put("userId", String.valueOf(userId));
            request.put("planDuration", planDuration.toLowerCase());
            request.put("amountRupees", amountRupees);

            System.out.println("🔄 Calling secure Edge Function for " + planDuration + " subscription...");
            ApiResponse response = apiService.callSupabaseFunction("create-razorpay-link", request);

            if (!response.isSuccess()) {
                System.err.println("Edge Function Error: " + response.getBody());
                throw new RuntimeException(
                        "Failed to create secure payment link. Please check your internet connection.");
            }

            JSONObject json = response.getJson();
            return json.getString("checkout_url");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating subscription: " + e.getMessage());
        }
    }

    /**
     * Create a pending subscription for a new user
     */
    public void createPendingSubscription(String userId, String email) {
        try {
            String endpoint = "/subscriptions/init";

            JSONObject request = new JSONObject();
            request.put("userId", userId);
            request.put("email", email);

            System.out.println("Initiating pending subscription for user: " + userId);
            ApiResponse response = apiService.post(endpoint, request);

            if (!response.isSuccess()) {
                System.err.println("Failed to init subscription: " + response.getBody());
            } else {
                System.out.println("Successfully initiated pending subscription");
            }
        } catch (Exception e) {
            System.err.println("Error initiating pending subscription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fetch all subscription prices from backend
     */
    public Map<String, String> getPricing() {
        Map<String, String> prices = new HashMap<>();
        try {
            ApiResponse response = apiService.get("/pricing");
            if (response.isSuccess()) {
                JSONArray array = new JSONArray(response.getBody());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String duration = obj.getString("planDuration");
                    long amountRupees = obj.getLong("amountPaise");
                    prices.put(duration, "₹" + amountRupees);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching prices: " + e.getMessage());
        }

        // Return default values if fetch fails
        if (!prices.containsKey("monthly"))
            prices.put("monthly", "₹1");
        if (!prices.containsKey("yearly"))
            prices.put("yearly", "₹1");

        return prices;
    }

    /**
     * Get current logged-in user's email
     */
    private String getCurrentUserEmail() {
        try {
            com.mahal.util.SessionManager sessionManager = SessionManager.getInstance();
            if (sessionManager.isLoggedIn() && sessionManager.getCurrentUser() != null) {
                return sessionManager.getCurrentUser().getEmail();
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return null;
    }

    /**
     * Get current logged-in user's ID
     */
    private Long getCurrentUserId() {
        try {
            com.mahal.util.SessionManager sessionManager = SessionManager.getInstance();
            if (sessionManager.isLoggedIn() && sessionManager.getCurrentUser() != null) {
                return sessionManager.getCurrentUser().getId();
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return null;
    }

    /**
     * Subscription status data class
     */
    public static class SubscriptionStatus {
        private final boolean active;
        private final String status;
        private final String planDuration;
        private final LocalDateTime endDate;

        public SubscriptionStatus(boolean active, String status, String planDuration, LocalDateTime endDate) {
            this.active = active;
            this.status = status;
            this.planDuration = planDuration;
            this.endDate = endDate;
        }

        public boolean isActive() {
            return active;
        }

        public String getStatus() {
            return status;
        }

        public String getPlanDuration() {
            return planDuration;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }
    }
}
