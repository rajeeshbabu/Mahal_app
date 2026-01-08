package com.mahal.subscription.service;

import com.mahal.subscription.model.Subscription;
import com.mahal.sync.SupabaseSyncService;
import org.json.JSONObject;

/**
 * Helper class to convert Subscription entity to JSON and sync to Supabase.
 */
public class SubscriptionSyncHelper {

    private static final java.time.format.DateTimeFormatter DATE_TIME_FORMATTER = java.time.format.DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final java.time.ZoneId IST_ZONE = java.time.ZoneId.of("Asia/Kolkata");

    /**
     * Get current time in Indian Standard Time (IST) for consistency.
     */
    public static java.time.LocalDateTime getNowUtc() {
        return java.time.LocalDateTime.now(IST_ZONE);
    }

    /**
     * Robust date parsing for ISO 8601 strings from various sources.
     */
    public static java.time.LocalDateTime parseIsoDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "null".equals(dateStr))
            return null;
        try {
            // 1. Clean the string
            String cleanDate = dateStr.trim();
            if (cleanDate.contains("+")) {
                cleanDate = cleanDate.substring(0, cleanDate.indexOf("+"));
            }
            if (cleanDate.endsWith("Z")) {
                cleanDate = cleanDate.substring(0, cleanDate.length() - 1);
            }

            // 2. Standardize separator to space (to match our DATE_TIME_FORMATTER)
            cleanDate = cleanDate.replace("T", " ");

            // 3. Handle milliseconds carefully
            if (cleanDate.contains(".")) {
                int dotIdx = cleanDate.indexOf(".");
                String datePart = cleanDate.substring(0, dotIdx);
                String msPart = cleanDate.substring(dotIdx + 1);

                // Ensure exactly 3-digit milliseconds for our formatter
                if (msPart.length() < 3) {
                    msPart = String.format("%-3s", msPart).replace(' ', '0');
                } else if (msPart.length() > 3) {
                    msPart = msPart.substring(0, 3);
                }
                cleanDate = datePart + "." + msPart;
            } else {
                // Add missing milliseconds
                cleanDate += ".000";
            }

            return java.time.LocalDateTime.parse(cleanDate, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            System.err.println("⚠️  Robust parse failed for '" + dateStr + "': " + e.getMessage());
            try {
                // Fallback 1: Try replacing space with T and using standard ISO parser
                return java.time.LocalDateTime.parse(dateStr.replace(" ", "T"));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Convert Subscription entity to JSON string for Supabase sync.
     * Note: We don't include 'id' in the JSON because Supabase will auto-generate
     * it.
     */
    public static String subscriptionToJson(Subscription subscription) {
        JSONObject json = new JSONObject();
        // Don't include 'id' - let Supabase auto-generate it
        // json.put("id", subscription.getId());

        json.put("user_id", subscription.getUserId());
        if (subscription.getUserEmail() != null && !subscription.getUserEmail().isEmpty()) {
            json.put("user_email", subscription.getUserEmail());
        }
        json.put("plan_duration", subscription.getPlanDuration());
        json.put("status", subscription.getStatus());

        if (subscription.getStartDate() != null) {
            json.put("start_date", subscription.getStartDate().format(DATE_TIME_FORMATTER));
        }
        if (subscription.getEndDate() != null) {
            json.put("end_date", subscription.getEndDate().format(DATE_TIME_FORMATTER));
        }
        if (subscription.getRazorpaySubscriptionId() != null && !subscription.getRazorpaySubscriptionId().isEmpty()) {
            json.put("razorpay_subscription_id", subscription.getRazorpaySubscriptionId());
        }
        if (subscription.getSuperadminStatus() != null) {
            json.put("superadmin_status", subscription.getSuperadminStatus());
        }
        if (subscription.getCreatedAt() != null) {
            json.put("created_at", subscription.getCreatedAt().format(DATE_TIME_FORMATTER));
        }
        if (subscription.getUpdatedAt() != null) {
            json.put("updated_at", subscription.getUpdatedAt().format(DATE_TIME_FORMATTER));
        }

        return json.toString();
    }

    /**
     * Convert Supabase JSON to Subscription entity.
     */
    public static void jsonToSubscription(JSONObject json, Subscription subscription) {
        if (json.has("user_id")) {
            subscription.setUserId(json.getString("user_id"));
        }
        if (json.has("user_email") && !json.isNull("user_email")) {
            subscription.setUserEmail(json.getString("user_email"));
        }
        if (json.has("plan_duration")) {
            subscription.setPlanDuration(json.getString("plan_duration"));
        }
        if (json.has("status")) {
            subscription.setStatus(json.getString("status"));
        }
        if (json.has("superadmin_status") && !json.isNull("superadmin_status")) {
            subscription.setSuperadminStatus(json.getString("superadmin_status"));
        }

        try {
            if (json.has("start_date") && !json.isNull("start_date")) {
                subscription.setStartDate(parseIsoDateTime(json.getString("start_date")));
            }
            if (json.has("end_date") && !json.isNull("end_date")) {
                subscription.setEndDate(parseIsoDateTime(json.getString("end_date")));
            }
            if (json.has("created_at") && !json.isNull("created_at")) {
                subscription.setCreatedAt(parseIsoDateTime(json.getString("created_at")));
            }
            if (json.has("updated_at") && !json.isNull("updated_at")) {
                subscription.setUpdatedAt(parseIsoDateTime(json.getString("updated_at")));
            }
        } catch (Exception e) {
            System.err.println("Error parsing dates from Supabase: " + e.getMessage());
        }

        if (json.has("razorpay_subscription_id") && !json.isNull("razorpay_subscription_id")) {
            subscription.setRazorpaySubscriptionId(json.getString("razorpay_subscription_id"));
        }
    }

    /**
     * Sync subscription to Supabase (insert or update).
     */
    public static void syncSubscription(SupabaseSyncService syncService, Subscription subscription, String operation) {
        if (syncService == null || !syncService.isConfigured()) {
            System.err.println("⚠️  Supabase not configured - skipping sync");
            return;
        }

        try {
            String jsonData = subscriptionToJson(subscription);
            String userId = subscription.getUserId();
            String tableName = "subscriptions";
            String existingId = syncService.fetchLatestSubscriptionId(userId);

            // Now that SupabaseSyncService.insert() is an UPSERT (using
            // on_conflict=user_id),
            // we don't need to check for existing records anymore. We just "INSERT" it.
            // This is the most reliable way to sync status changes.

            System.out.println("🔄 Syncing subscription to Supabase via UPSERT (user: " + userId + ")");

            boolean success = false;
            if ("DELETE".equals(operation)) {
                // For DELETE we still need to fetch ID or use userId
                String deleteId = existingId != null ? existingId : String.valueOf(subscription.getId());
                success = syncService.delete(tableName, deleteId, userId);
            } else {
                // For INSERT and UPDATE, just use the UPSERT-capable insert method
                success = syncService.insert(tableName, jsonData, userId);
            }

            if (success) {
                System.out.println("✅ Successfully synced subscription to Supabase");
            } else {
                System.err.println("❌ Failed to sync subscription to Supabase");
            }
        } catch (Exception e) {
            System.err.println("❌ Error syncing subscription to Supabase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
