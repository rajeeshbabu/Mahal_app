package com.mahal.subscription.service;

import com.mahal.subscription.dto.SubscriptionResponse;
import com.mahal.subscription.model.Subscription;
import com.mahal.subscription.repository.SubscriptionRepository;
import com.mahal.subscription.service.SubscriptionSyncHelper;
import com.mahal.sync.SupabaseSyncService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RazorpaySubscriptionService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.base.url:https://api.razorpay.com/v1}")
    private String razorpayBaseUrl;

    @Value("${razorpay.mock.enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private com.mahal.subscription.repository.SubscriptionPricingRepository pricingRepository;

    @Autowired(required = false)
    private SupabaseSyncService supabaseSyncService;

    /**
     * Create a subscription in Razorpay
     * Returns hosted checkout URL for payment
     */
    public SubscriptionResponse createSubscription(String userId, String planDuration) {
        // Use mock mode if enabled
        if (mockEnabled) {
            return createMockSubscription(userId, planDuration);
        }

        // Validate Razorpay credentials
        if (razorpayKeyId == null || razorpayKeyId.equals("your_razorpay_key_id") ||
                razorpayKeySecret == null || razorpayKeySecret.equals("your_razorpay_key_secret")) {
            throw new RuntimeException(
                    "Razorpay API keys not configured. Please set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables or update application.properties. See RAZORPAY_SETUP.md for instructions.");
        }

        try {
            // Fetch plan amount from database
            com.mahal.subscription.model.SubscriptionPricing pricing = pricingRepository
                    .findByPlanDuration(planDuration.toLowerCase())
                    .orElseThrow(() -> new RuntimeException("Pricing not found for plan: " + planDuration));

            long amountInRupees = pricing.getAmountPaise();
            long amountInPaise = amountInRupees * 100; // DB stores Rupees, Razorpay needs Paise

            // 1. Create Payment Link (One-time payment, no auto-pay/mandate)
            JSONObject paymentLinkData = new JSONObject();
            paymentLinkData.put("amount", amountInPaise);
            paymentLinkData.put("currency", "INR");
            paymentLinkData.put("accept_partial", false);
            paymentLinkData.put("description", "Mahal Management System - " +
                    planDuration.substring(0, 1).toUpperCase() + planDuration.substring(1));

            // Customer details
            JSONObject customer = new JSONObject();
            customer.put("email", userId + "@mahalapp.com");
            paymentLinkData.put("customer", customer);

            // Notification settings
            JSONObject notify = new JSONObject();
            notify.put("email", true);
            notify.put("sms", false);
            paymentLinkData.put("notify", notify);

            paymentLinkData.put("reminder_enable", true);

            // CRITICAL: Notes are used by the Webhook to identify the user
            JSONObject notes = new JSONObject();
            notes.put("user_id", userId);
            notes.put("plan_duration", planDuration.toLowerCase());
            paymentLinkData.put("notes", notes);

            // Make API call to Razorpay Payment Links API
            System.out.println("🔄 [RAZORPAY] Creating one-time payment link for user: " + userId);
            String response = makeRazorpayRequest("POST", "/payment_links", paymentLinkData.toString());
            JSONObject razorpayResponse = new JSONObject(response);

            String paymentLinkId = razorpayResponse.getString("id");
            String checkoutUrl = razorpayResponse.getString("short_url");

            // Log full response for debugging
            System.out.println("📋 Razorpay Response: " + razorpayResponse.toString(2));
            System.out.println("✅ Payment Link created: " + paymentLinkId);
            System.out.println("✅ Checkout URL: " + checkoutUrl);

            // Save/Update subscription record in database as "pending"
            java.util.Optional<Subscription> existingOpt = subscriptionRepository
                    .findTopByUserIdOrderByCreatedAtDesc(userId);

            Subscription subscription;
            if (existingOpt.isPresent()) {
                subscription = existingOpt.get();
                // Verify it's actually the pending one (logic can be refined if multiple
                // history exists)
                // But for now, just update the latest one if it's pending or created
                if (!"active".equalsIgnoreCase(subscription.getStatus())) {
                    System.out.println("📝 Updating existing pending subscription (ID: " + subscription.getId() + ")");
                } else {
                    // If active, we might be creating a NEW subscription (e.g. renewal/upgrade),
                    // but for this flow it's likely the initial one.
                    // Let's create new if the latest is active to allow re-subscription.
                    subscription = new Subscription();
                    subscription.setUserId(userId);
                    // userEmail will be set if we can find it, otherwise it's nullable in DB
                    System.out.println("📝 Creating NEW subscription as previous was active");
                }
            } else {
                subscription = new Subscription();
                subscription.setUserId(userId);
                // userEmail will be set if we can find it, otherwise it's nullable in DB
            }

            subscription.setPlanDuration(planDuration);
            // Don't overwrite status if it was already "pending" - just keep it pending
            if (subscription.getStatus() == null) {
                subscription.setStatus("pending");
            }
            subscription.setRazorpaySubscriptionId(paymentLinkId);
            subscription.setStartDate(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());
            subscription = subscriptionRepository.save(subscription);

            // Don't sync to Supabase when status is "pending" - only sync when it becomes
            // "active"
            System.out.println(
                    "📝 Subscription created with status 'pending' - will sync to Supabase when status becomes 'active'");

            SubscriptionResponse responseDto = new SubscriptionResponse();
            responseDto.setCheckoutUrl(checkoutUrl);
            responseDto.setSubscriptionId(paymentLinkId);
            responseDto.setStatus("pending");

            return responseDto;
        } catch (Exception e) {
            System.err.println("❌ ERROR in createSubscription:");
            e.printStackTrace();
            throw new RuntimeException("Failed to create subscription: " + e.getMessage());
        }
    }

    /**
     * Create or get existing Razorpay plan
     */
    private String createOrGetPlan(String planDuration, long amountInPaise, int intervalCount) {
        try {
            // Try to find existing plan first
            String planName = "mahal_" + planDuration + "_plan";

            // Create plan in Razorpay
            JSONObject planData = new JSONObject();
            planData.put("period", "monthly");
            planData.put("interval", intervalCount);
            planData.put("item", new JSONObject()
                    .put("name",
                            "Mahal Management System - " + planDuration.substring(0, 1).toUpperCase()
                                    + planDuration.substring(1))
                    .put("amount", amountInPaise)
                    .put("currency", "INR")
                    .put("description", "Full access to Mahal Management System"));

            // Restrict payment methods to UPI and Cards only
            JSONObject paymentMethods = new JSONObject();
            paymentMethods.put("card", true);
            paymentMethods.put("upi", true);
            paymentMethods.put("wallet", false);
            paymentMethods.put("netbanking", false);
            paymentMethods.put("cod", false);
            planData.put("notes", new JSONObject().put("plan_name", planName));

            String response = makeRazorpayRequest("POST", "/plans", planData.toString());
            JSONObject planResponse = new JSONObject(response);

            return planResponse.getString("id");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create plan: " + e.getMessage());
        }
    }

    /**
     * Make authenticated request to Razorpay API
     */
    /**
     * Make HTTP request to Razorpay API
     * Made package-private so SubscriptionService can use it
     */
    String makeRazorpayRequest(String method, String endpoint, String body) throws Exception {
        URL url = new java.net.URI(razorpayBaseUrl + endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");

        // Basic Authentication
        String auth = razorpayKeyId + ":" + razorpayKeySecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

        if (body != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (responseCode >= 200 && responseCode < 300) {
            return response.toString();
        } else {
            throw new RuntimeException("Razorpay API error: " + responseCode + " - " + response.toString());
        }
    }

    /**
     * Create a mock subscription for testing (no Razorpay API calls)
     * Returns a fake checkout URL that can be used for testing
     */
    private SubscriptionResponse createMockSubscription(String userId, String planDuration) {
        try {
            // Generate fake subscription ID
            String fakeSubscriptionId = "sub_mock_" + System.currentTimeMillis();

            // Create a mock checkout URL (points to a test page)
            String mockCheckoutUrl = "http://localhost:8080/mock-payment.html?subscription_id=" + fakeSubscriptionId +
                    "&user_id=" + userId + "&plan=" + planDuration;

            // Save subscription record in database with "pending" status
            // Save subscription record in database
            // Check for existing pending subscription first
            java.util.Optional<Subscription> existingOpt = subscriptionRepository
                    .findTopByUserIdOrderByCreatedAtDesc(userId);

            Subscription subscription;
            if (existingOpt.isPresent()) {
                subscription = existingOpt.get();
                if (!"active".equalsIgnoreCase(subscription.getStatus())) {
                    System.out.println(
                            "📝 MOCK: Updating existing pending subscription (ID: " + subscription.getId() + ")");
                } else {
                    subscription = new Subscription();
                    subscription.setUserId(userId);
                    subscription.setUserEmail(userId);
                    System.out.println("📝 MOCK: Creating NEW subscription as previous was active");
                }
            } else {
                subscription = new Subscription();
                subscription.setUserId(userId);
                subscription.setUserEmail(userId);
            }

            subscription.setPlanDuration(planDuration);
            if (subscription.getStatus() == null) {
                subscription.setStatus("pending");
            }
            subscription.setStartDate(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());
            subscription = subscriptionRepository.save(subscription);

            // Don't sync to Supabase when status is "pending" - only sync when it becomes
            // "active"
            System.out.println(
                    "📝 Mock subscription created with status 'pending' - will sync to Supabase when status becomes 'active'");

            SubscriptionResponse responseDto = new SubscriptionResponse();
            responseDto.setCheckoutUrl(mockCheckoutUrl);
            responseDto.setSubscriptionId(fakeSubscriptionId);
            responseDto.setStatus("pending");

            System.out
                    .println("⚠️  MOCK MODE: Created fake subscription " + fakeSubscriptionId + " for user " + userId);
            System.out.println("⚠️  MOCK MODE: Checkout URL: " + mockCheckoutUrl);
            System.out.println(
                    "⚠️  MOCK MODE: To activate subscription, manually update status in database or use webhook simulator");

            return responseDto;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create mock subscription: " + e.getMessage());
        }
    }
}
