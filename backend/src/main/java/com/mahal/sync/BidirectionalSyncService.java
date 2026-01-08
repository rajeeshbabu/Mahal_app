package com.mahal.sync;

import com.mahal.subscription.model.Subscription;
import com.mahal.subscription.repository.SubscriptionRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Service for bidirectional sync between Supabase and local SQLite database.
 * - Pulls changes from Supabase to local database
 * - Ensures local changes are synced to Supabase
 */
@Service
public class BidirectionalSyncService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired(required = false)
    private SupabaseSyncService supabaseSyncService;

    /**
     * Pull subscriptions from Supabase and sync to local database.
     * Manual trigger or specific user context only.
     */
    public void pullFromSupabase() {
        if (supabaseSyncService == null || !supabaseSyncService.isConfigured()) {
            return; // Supabase not configured, skip
        }

        try {
            System.out.println("🔄 Starting sync from Supabase to local database...");

            // Get all subscriptions from Supabase
            List<Subscription> supabaseSubscriptions = fetchSubscriptionsFromSupabase();

            if (supabaseSubscriptions == null || supabaseSubscriptions.isEmpty()) {
                System.out.println("No subscriptions found in Supabase.");
                return;
            }

            int syncedCount = 0;
            int pushedCount = 0;
            for (Subscription supabaseSub : supabaseSubscriptions) {
                // Check if subscription exists in local database using user_id
                Optional<Subscription> localSubOpt = subscriptionRepository
                        .findTopByUserIdOrderByCreatedAtDesc(supabaseSub.getUserId());

                if (localSubOpt.isPresent()) {
                    Subscription localSub = localSubOpt.get();

                    // CASE 1: Supabase is newer - Update Local
                    if (shouldUpdateLocal(supabaseSub, localSub)) {
                        System.out.println("⬇️ Supabase version is newer for " + localSub.getUserId() +
                                " (S:" + supabaseSub.getUpdatedAt() + " > L:" + localSub.getUpdatedAt() + ")");
                        updateLocalSubscription(supabaseSub, localSub);
                        syncedCount++;
                    }
                    // CASE 2: Local is newer - Push to Supabase
                    else if (shouldUpdateSupabase(localSub, supabaseSub)) {
                        System.out.println("⬆️ Local version is newer for " + localSub.getUserId() +
                                " (L:" + localSub.getUpdatedAt() + " > S:" + supabaseSub.getUpdatedAt() + ")");
                        pushToSupabase(localSub);
                        pushedCount++;
                    }
                } else {
                    // CASE 3: Not in local - Insert new from Supabase
                    subscriptionRepository.save(supabaseSub);
                    syncedCount++;
                    System.out.println("✓ Pulled new subscription from Supabase for user: " + supabaseSub.getUserId());
                }
            }

            // Phase 2: Push local-only records to Supabase
            pushedCount += pushNewLocalRecords(supabaseSubscriptions);

            System.out.println("✅ Sync complete. Pulled: " + syncedCount + ", Pushed: " + pushedCount);
        } catch (Exception e) {
            System.err.println("✗ Error pulling from Supabase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fetch all subscriptions from Supabase.
     */
    private List<Subscription> fetchSubscriptionsFromSupabase() {
        int maxRetries = 3;
        int retryDelayMs = 2000;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String supabaseUrl = getSupabaseUrl();
                String apiKey = getSupabaseApiKey();

                if (supabaseUrl == null || apiKey == null) {
                    System.err.println("✗ Supabase URL or API Key missing in sync service.");
                    return null;
                }

                String fullUrl = supabaseUrl + "/rest/v1/subscriptions?select=*&order=updated_at.desc";
                System.out.println("🔄 [SYNC] Attempting to fetch from Supabase (Attempt " + attempt + "): " + fullUrl);

                URL url = new java.net.URI(fullUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", apiKey);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);

                // Increase timeouts for large data sets or slow networks
                conn.setConnectTimeout(60000); // 60 seconds
                conn.setReadTimeout(60000); // 60 seconds

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    return parseSubscriptionsFromJson(jsonArray);
                } else {
                    System.err.println("✗ Supabase sync attempt " + attempt + " failed: HTTP " + responseCode);
                    if (responseCode >= 500) {
                        // Server error, worth retrying
                        Thread.sleep(retryDelayMs * attempt);
                        continue;
                    }
                    return null;
                }
            } catch (Exception e) {
                lastException = e;
                System.err.println("⚠️ [SYNC] Attempt " + attempt + " failed for Supabase fetch: " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (lastException != null) {
            System.err.println(
                    "❌ [SYNC] All " + maxRetries + " attempts failed. Last error: " + lastException.getMessage());
        }
        return null;
    }

    /**
     * Parse JSON array to Subscription entities.
     */
    private List<Subscription> parseSubscriptionsFromJson(JSONArray jsonArray) {
        List<Subscription> subscriptions = new java.util.ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject json = jsonArray.getJSONObject(i);
                Subscription sub = new Subscription();

                if (json.has("id") && !json.isNull("id")) {
                    sub.setId(json.getLong("id"));
                }
                if (json.has("user_id") && !json.isNull("user_id")) {
                    sub.setUserId(json.getString("user_id"));
                }
                if (json.has("user_email") && !json.isNull("user_email")) {
                    sub.setUserEmail(json.getString("user_email"));
                }
                if (json.has("plan_duration") && !json.isNull("plan_duration")) {
                    sub.setPlanDuration(json.getString("plan_duration"));
                }
                if (json.has("status") && !json.isNull("status")) {
                    sub.setStatus(json.getString("status"));
                }
                if (json.has("razorpay_subscription_id") && !json.isNull("razorpay_subscription_id")) {
                    sub.setRazorpaySubscriptionId(json.getString("razorpay_subscription_id"));
                }

                if (json.has("start_date") && !json.isNull("start_date")) {
                    sub.setStartDate(com.mahal.subscription.service.SubscriptionSyncHelper
                            .parseIsoDateTime(json.getString("start_date")));
                }
                if (json.has("end_date") && !json.isNull("end_date")) {
                    sub.setEndDate(com.mahal.subscription.service.SubscriptionSyncHelper
                            .parseIsoDateTime(json.getString("end_date")));
                }
                if (json.has("created_at") && !json.isNull("created_at")) {
                    sub.setCreatedAt(com.mahal.subscription.service.SubscriptionSyncHelper
                            .parseIsoDateTime(json.getString("created_at")));
                }
                if (json.has("updated_at") && !json.isNull("updated_at")) {
                    sub.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper
                            .parseIsoDateTime(json.getString("updated_at")));
                }

                subscriptions.add(sub);
            } catch (Exception e) {
                System.err.println("✗ Error parsing subscription from JSON: " + e.getMessage());
            }
        }

        return subscriptions;
    }

    /**
     * Check if local subscription should be updated from Supabase version.
     */
    private boolean shouldUpdateLocal(Subscription supabaseSub, Subscription localSub) {
        // Update if Supabase version has a newer updated_at timestamp
        if (supabaseSub.getUpdatedAt() != null && localSub.getUpdatedAt() != null) {
            return supabaseSub.getUpdatedAt().isAfter(localSub.getUpdatedAt());
        }
        // If local is missing updated_at but supabase has it, pull it
        return supabaseSub.getUpdatedAt() != null;
    }

    /**
     * Check if local subscription should be pushed to Supabase.
     */
    private boolean shouldUpdateSupabase(Subscription localSub, Subscription supabaseSub) {
        if (localSub.getUpdatedAt() != null && supabaseSub.getUpdatedAt() != null) {
            return localSub.getUpdatedAt().isAfter(supabaseSub.getUpdatedAt());
        }
        return false;
    }

    /**
     * Phase 2: Find local records that don't exist in the provided Supabase list
     * and push them.
     */
    private int pushNewLocalRecords(List<Subscription> supabaseList) {
        int count = 0;
        try {
            List<Subscription> localSubs = subscriptionRepository.findAll();
            for (Subscription local : localSubs) {
                boolean existsInfo = supabaseList.stream()
                        .anyMatch(s -> s.getUserId().equals(local.getUserId()));

                if (!existsInfo) {
                    System.out.println(
                            "⬆️ Local-only record found for " + local.getUserId() + ". Pushing to Supabase...");
                    pushToSupabase(local);
                    count++;
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Error in pushNewLocalRecords: " + e.getMessage());
        }
        return count;
    }

    /**
     * Push local subscription to Supabase.
     */
    private void pushToSupabase(Subscription localSub) {
        if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
            com.mahal.subscription.service.SubscriptionSyncHelper.syncSubscription(supabaseSyncService, localSub,
                    "UPDATE");
        }
    }

    /**
     * Update local subscription with data from Supabase.
     */
    private void updateLocalSubscription(Subscription supabaseSub, Subscription localSub) {
        localSub.setStatus(supabaseSub.getStatus());
        localSub.setPlanDuration(supabaseSub.getPlanDuration());
        localSub.setStartDate(supabaseSub.getStartDate());
        localSub.setEndDate(supabaseSub.getEndDate());
        localSub.setUserEmail(supabaseSub.getUserEmail());

        // Use the Supabase timestamp to ensure they stay in sync
        if (supabaseSub.getUpdatedAt() != null) {
            localSub.setUpdatedAt(supabaseSub.getUpdatedAt());
        } else {
            localSub.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
        }

        subscriptionRepository.save(localSub);
        System.out.println("✓ Updated local subscription from Supabase for user: " + localSub.getUserId());
    }

    /**
     * Get Supabase URL from SupabaseSyncService.
     */
    private String getSupabaseUrl() {
        if (supabaseSyncService == null || !supabaseSyncService.isConfigured()) {
            return null;
        }
        return supabaseSyncService.getSupabaseUrl();
    }

    /**
     * Get Supabase API key from SupabaseSyncService.
     */
    private String getSupabaseApiKey() {
        if (supabaseSyncService == null || !supabaseSyncService.isConfigured()) {
            return null;
        }
        return supabaseSyncService.getSupabaseApiKey();
    }
}
