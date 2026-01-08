package com.mahal.subscription.controller;

import com.mahal.subscription.repository.SubscriptionRepository;
import com.mahal.subscription.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mock payment controller for testing subscriptions without real Razorpay
 * integration
 * Only active when razorpay.mock.enabled=true
 */
@RestController
@RequestMapping("/mock-payment")
@CrossOrigin(origins = "*")
public class MockPaymentController {

    @Value("${razorpay.mock.enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    /**
     * Mock payment page - simulates Razorpay checkout
     * In a real scenario, this would be handled by Razorpay's hosted page
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> mockPaymentPage(
            @RequestParam String subscription_id,
            @RequestParam String user_id,
            @RequestParam String plan) {

        if (!mockEnabled) {
            Map<String, Object> error = new HashMap<>();
            error.put("error",
                    "Mock payment is disabled. Enable razorpay.mock.enabled=true in application.properties to use mock payments.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("subscription_id", subscription_id);
        response.put("user_id", user_id);
        response.put("plan", plan);
        response.put("message",
                "This is a MOCK payment page. In production, this would be Razorpay's hosted checkout.");
        response.put("instructions",
                "To simulate payment success, call: POST /mock-payment/success?subscription_id=" + subscription_id);

        return ResponseEntity.ok(response);
    }

    /**
     * Simulate successful payment
     * This activates the subscription in the database
     */
    @PostMapping("/success")
    public ResponseEntity<Map<String, Object>> simulatePaymentSuccess(
            @RequestParam String subscription_id) {

        if (!mockEnabled) {
            Map<String, Object> error = new HashMap<>();
            error.put("error",
                    "Mock payment is disabled. Enable razorpay.mock.enabled=true in application.properties to use mock payments.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        try {
            // Get subscription to determine plan duration
            Optional<com.mahal.subscription.model.Subscription> subscriptionOpt = subscriptionRepository
                    .findByRazorpaySubscriptionId(subscription_id);

            if (subscriptionOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Subscription not found: " + subscription_id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            com.mahal.subscription.model.Subscription subscription = subscriptionOpt.get();
            String planDuration = subscription.getPlanDuration();

            // Calculate end date based on plan duration
            LocalDateTime endDate;
            if ("yearly".equals(planDuration)) {
                endDate = LocalDateTime.now().plusYears(1);
            } else {
                endDate = LocalDateTime.now().plusMonths(1); // monthly
            }

            subscriptionService.updateSubscriptionStatus(subscription_id, "active", endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mock payment successful! Subscription activated.");
            response.put("subscription_id", subscription_id);
            response.put("status", "active");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to activate subscription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Simulate payment failure
     */
    @PostMapping("/failure")
    public ResponseEntity<Map<String, Object>> simulatePaymentFailure(
            @RequestParam String subscription_id) {

        if (!mockEnabled) {
            Map<String, Object> error = new HashMap<>();
            error.put("error",
                    "Mock payment is disabled. Enable razorpay.mock.enabled=true in application.properties to use mock payments.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        try {
            // Update subscription status to cancelled/failed
            subscriptionService.updateSubscriptionStatus(subscription_id, "cancelled", null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Mock payment failed! Subscription cancelled.");
            response.put("subscription_id", subscription_id);
            response.put("status", "cancelled");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to update subscription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
