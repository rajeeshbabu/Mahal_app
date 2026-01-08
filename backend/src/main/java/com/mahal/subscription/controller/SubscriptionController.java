package com.mahal.subscription.controller;

import com.mahal.subscription.dto.CreateSubscriptionRequest;
import com.mahal.subscription.dto.SubscriptionResponse;
import com.mahal.subscription.dto.SubscriptionStatusResponse;
import com.mahal.subscription.model.Subscription;
import com.mahal.subscription.service.RazorpaySubscriptionService;
import com.mahal.subscription.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private RazorpaySubscriptionService razorpayService;

    /**
     * Check subscription status for the current user
     */
    @GetMapping("/status")
    public ResponseEntity<SubscriptionStatusResponse> getSubscriptionStatus(
            Principal principal,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email) {
        // Priority: 1) userId query parameter, 2) Principal (if authenticated), 3)
        // "test_user" fallback
        String userIdentifier;
        if (userId != null && !userId.isEmpty()) {
            userIdentifier = userId;
        } else if (principal != null) {
            userIdentifier = principal.getName();
        } else {
            userIdentifier = "test_user";
        }

        System.out.println("SubscriptionController.getSubscriptionStatus called with userId=" + userId +
                ", email=" + email +
                ", principal=" + (principal != null ? principal.getName() : "null") +
                ", using userIdentifier=" + userIdentifier);

        // If email is provided, we use it for better validation in the service
        SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(userIdentifier, email);
        System.out.println(
                "Subscription status response: [active=" + status.isActive() + ", status='" + status.getStatus()
                        + "', endDate=" + status.getEndDate() + "]");

        return ResponseEntity.ok(status);
    }

    /**
     * Initialize a pending subscription for a new user
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initSubscription(
            @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String email = request.get("email");

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "UserId is required"));
        }

        Subscription subscription = subscriptionService.createPendingSubscription(userId, email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("subscriptionId", subscription.getId());
        response.put("status", subscription.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new subscription (Monthly or Yearly)
     * Returns Razorpay hosted checkout URL
     */
    @PostMapping("/create")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @RequestBody CreateSubscriptionRequest request,
            Principal principal,
            @RequestParam(required = false) String userId) {
        // Priority: 1) userId query parameter, 2) Principal (if authenticated), 3)
        // "test_user" fallback
        String userIdentifier;
        if (userId != null && !userId.isEmpty()) {
            userIdentifier = userId;
        } else if (principal != null) {
            userIdentifier = principal.getName();
        } else {
            userIdentifier = "test_user";
        }

        // Validate plan duration
        if (!request.getPlanDuration().equals("monthly") && !request.getPlanDuration().equals("yearly")) {
            return ResponseEntity.badRequest().build();
        }

        SubscriptionResponse response = razorpayService.createSubscription(userIdentifier, request.getPlanDuration());
        return ResponseEntity.ok(response);
    }

    /**
     * Get subscription details
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getSubscriptionDetails(
            Principal principal,
            @RequestParam(required = false) String userId) {
        // Priority: 1) userId query parameter, 2) Principal (if authenticated), 3)
        // "test_user" fallback
        String userIdentifier;
        if (userId != null && !userId.isEmpty()) {
            userIdentifier = userId;
        } else if (principal != null) {
            userIdentifier = principal.getName();
        } else {
            userIdentifier = "test_user";
        }

        Map<String, Object> details = subscriptionService.getSubscriptionDetails(userIdentifier);
        return ResponseEntity.ok(details);
    }

    /**
     * Delete all subscriptions (for testing/reset purposes)
     * WARNING: This will delete ALL subscription records!
     */
    @DeleteMapping("/delete-all")
    public ResponseEntity<Map<String, Object>> deleteAllSubscriptions() {
        try {
            long count = subscriptionService.deleteAllSubscriptions();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deleted " + count + " subscription(s)");
            response.put("deletedCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
