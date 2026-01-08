package com.mahal.subscription.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookService {

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Parse webhook payload from Razorpay
     */
    public Map<String, Object> parseWebhookPayload(String payload) {
        JSONObject json = new JSONObject(payload);
        Map<String, Object> event = new HashMap<>();

        event.put("event", json.optString("event"));
        event.put("payload", json.optJSONObject("payload"));

        return event;
    }

    /**
     * Handle subscription activated event
     */
    public void handleSubscriptionActivated(Map<String, Object> event) {
        JSONObject payload = (JSONObject) event.get("payload");
        JSONObject subscription = payload.getJSONObject("subscription");

        String razorpaySubscriptionId = subscription.getString("id");
        String planId = subscription.optString("plan_id", "");

        // Determine plan duration from plan_id or subscription details
        String planDuration = extractPlanDuration(planId, subscription);

        subscriptionService.activateSubscription(razorpaySubscriptionId, planDuration);
    }

    /**
     * Handle subscription charged (renewal) event
     */
    public void handleSubscriptionCharged(Map<String, Object> event) {
        JSONObject payload = (JSONObject) event.get("payload");
        JSONObject subscription = payload.getJSONObject("subscription");

        String razorpaySubscriptionId = subscription.getString("id");
        String planId = subscription.optString("plan_id", "");
        String planDuration = extractPlanDuration(planId, subscription);

        subscriptionService.renewSubscription(razorpaySubscriptionId, planDuration);
    }

    /**
     * Handle subscription cancelled event
     */
    public void handleSubscriptionCancelled(Map<String, Object> event) {
        JSONObject payload = (JSONObject) event.get("payload");
        JSONObject subscription = payload.getJSONObject("subscription");

        String razorpaySubscriptionId = subscription.getString("id");
        subscriptionService.cancelSubscription(razorpaySubscriptionId);
    }

    /**
     * Handle subscription paused event
     */
    public void handleSubscriptionPaused(Map<String, Object> event) {
        // Similar to cancelled - update status to paused
        JSONObject payload = (JSONObject) event.get("payload");
        JSONObject subscription = payload.getJSONObject("subscription");

        String razorpaySubscriptionId = subscription.getString("id");
        subscriptionService.cancelSubscription(razorpaySubscriptionId); // Treat paused as inactive
    }

    /**
     * Handle subscription resumed event
     */
    public void handleSubscriptionResumed(Map<String, Object> event) {
        handleSubscriptionActivated(event); // Same as activation
    }

    /**
     * Handle subscription completed event
     */
    public void handleSubscriptionCompleted(Map<String, Object> event) {
        // Subscription cycle completed - renew if auto-renewal is enabled
        handleSubscriptionCharged(event);
    }

    /**
     * Handle payment authorized event (fires immediately after successful payment)
     */
    public void handlePaymentAuthorized(Map<String, Object> event) {
        JSONObject payload = (JSONObject) event.get("payload");
        JSONObject payment = payload.getJSONObject("payment");
        JSONObject subscription = payment.optJSONObject("subscription");

        if (subscription != null) {
            String razorpaySubscriptionId = subscription.getString("id");
            String planId = subscription.optString("plan_id", "");
            String planDuration = extractPlanDuration(planId, subscription);

            System.out.println("Payment authorized for subscription: " + razorpaySubscriptionId);
            // Activate subscription immediately when payment is authorized
            subscriptionService.activateSubscription(razorpaySubscriptionId, planDuration);
        }
    }

    /**
     * Handle payment failed event
     */
    public void handlePaymentFailed(Map<String, Object> event) {
        JSONObject payload = (JSONObject) event.get("payload");
        JSONObject payment = payload.getJSONObject("payment");
        JSONObject subscription = payment.optJSONObject("subscription");

        if (subscription != null) {
            String razorpaySubscriptionId = subscription.getString("id");
            // Optionally mark subscription as payment_failed
            // For now, we'll keep it active but log the failure
            System.out.println("Payment failed for subscription: " + razorpaySubscriptionId);
        }
    }

    /**
     * Handle payment captured event (successful payment)
     * This is the most reliable event for confirming payment success
     */
    public void handlePaymentCaptured(Map<String, Object> event) {
        try {
            JSONObject payload = (JSONObject) event.get("payload");
            JSONObject paymentEntity = payload.getJSONObject("payment");
            JSONObject entity = paymentEntity.getJSONObject("entity");

            System.out.println("💰 Processing payment.captured event");
            System.out.println("   Payment ID: " + entity.optString("id", "N/A"));
            System.out.println("   Amount: " + entity.optInt("amount", 0) / 100.0 + " INR");
            System.out.println("   Status: " + entity.optString("status", "N/A"));

            // Check if this payment is for a subscription
            String razorpaySubscriptionId = entity.optString("subscription_id", "");

            if (razorpaySubscriptionId != null && !razorpaySubscriptionId.isEmpty()) {
                System.out.println("   Subscription ID: " + razorpaySubscriptionId);

                // Get plan information from notes or description
                JSONObject notes = entity.optJSONObject("notes");
                String planDuration = "monthly"; // default

                if (notes != null) {
                    planDuration = notes.optString("plan_duration", "monthly");
                    System.out.println("   Plan Duration (from notes): " + planDuration);
                }

                // Activate the subscription
                System.out.println("✅ Activating subscription due to successful payment capture");
                subscriptionService.activateSubscription(razorpaySubscriptionId, planDuration);
                System.out.println("✅ Subscription activated successfully via payment.captured webhook");
            } else {
                System.out.println("⚠️ Payment captured but no subscription_id found in payment entity");

                // FALLBACK: Check notes for user_id (Payment Link workflow)
                JSONObject notes = entity.optJSONObject("notes");
                if (notes != null && notes.has("user_id")) {
                    String userId = notes.getString("user_id");
                    String planDuration = notes.optString("plan_duration", "monthly");

                    System.out.println("✅ Found user_id in notes: " + userId + " (Plan: " + planDuration + ")");
                    System.out.println("🔄 Activating subscription via User ID fallback...");

                    subscriptionService.activateSubscriptionForUser(userId, planDuration);
                } else {
                    System.out.println("❌ No user_id found in notes either. Cannot identify user for activation.");
                    System.out.println("   Full entity: " + entity.toString(2));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling payment.captured event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract plan duration from plan ID or subscription object
     */
    private String extractPlanDuration(String planId, JSONObject subscription) {
        // Try to extract from plan_id (e.g., "plan_monthly_xxx" or "plan_yearly_xxx")
        if (planId.contains("monthly") || planId.contains("month")) {
            return "monthly";
        } else if (planId.contains("yearly") || planId.contains("year")) {
            return "yearly";
        }

        // Fallback: check subscription notes or other fields
        JSONObject notes = subscription.optJSONObject("notes");
        if (notes != null) {
            String duration = notes.optString("plan_duration", "");
            if (!duration.isEmpty()) {
                return duration;
            }
        }

        // Default to monthly if cannot determine
        return "monthly";
    }
}
