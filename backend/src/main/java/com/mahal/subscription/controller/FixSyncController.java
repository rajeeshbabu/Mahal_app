package com.mahal.subscription.controller;

import com.mahal.subscription.model.Subscription;
import com.mahal.subscription.repository.SubscriptionRepository;
import com.mahal.subscription.service.SubscriptionSyncHelper;
import com.mahal.sync.SupabaseSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for fixing sync issues between SQLite and Supabase.
 */
@RestController
@RequestMapping("/api/subscriptions/fix-sync")
@CrossOrigin(origins = "*")
public class FixSyncController {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired(required = false)
    private SupabaseSyncService supabaseSyncService;

    /**
     * Force sync a specific user's subscription from SQLite to Supabase.
     * POST /api/subscriptions/fix-sync/user?userEmail=suni@gmail.com
     */
    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> fixSyncForUser(@RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (supabaseSyncService == null || !supabaseSyncService.isConfigured()) {
                response.put("success", false);
                response.put("error", "Supabase not configured");
                return ResponseEntity.status(500).body(response);
            }

            String searchEmail = userEmail != null ? userEmail : userId;
            if (searchEmail == null || searchEmail.isEmpty()) {
                response.put("success", false);
                response.put("error", "userEmail or userId parameter is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Find subscription by user email or user id
            Optional<Subscription> subscriptionOpt = subscriptionRepository
                    .findTopByUserIdOrderByCreatedAtDesc(searchEmail);

            if (!subscriptionOpt.isPresent()) {
                // Try finding by user_email field
                List<Subscription> allSubs = subscriptionRepository.findAll();
                subscriptionOpt = allSubs.stream()
                        .filter(sub -> searchEmail.equals(sub.getUserEmail()) || searchEmail.equals(sub.getUserId()))
                        .findFirst();
            }

            if (!subscriptionOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Subscription not found for: " + searchEmail);
                return ResponseEntity.notFound().build();
            }

            Subscription subscription = subscriptionOpt.get();

            System.out.println("========================================");
            System.out.println("FIXING SYNC for: " + searchEmail);
            System.out.println("========================================");
            System.out.println("SQLite Data:");
            System.out.println("  ID: " + subscription.getId());
            System.out.println("  Status: " + subscription.getStatus());
            System.out.println("  Plan Duration: " + subscription.getPlanDuration());
            System.out.println("  Start Date: " + subscription.getStartDate());
            System.out.println("  End Date: " + subscription.getEndDate());
            System.out.println("  User Email: " + subscription.getUserEmail());
            System.out.println("  Razorpay ID: " + subscription.getRazorpaySubscriptionId());
            System.out.println("========================================");

            // Update the timestamp before syncing to Supabase
            subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
            subscription = subscriptionRepository.save(subscription);

            // Force update sync to Supabase (not INSERT, use UPDATE to overwrite)
            SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "UPDATE");

            response.put("success", true);
            response.put("message", "Subscription synced to Supabase");
            response.put("subscription", Map.of(
                    "id", subscription.getId(),
                    "userId", subscription.getUserId(),
                    "userEmail", subscription.getUserEmail() != null ? subscription.getUserEmail() : "",
                    "status", subscription.getStatus(),
                    "planDuration", subscription.getPlanDuration(),
                    "startDate", subscription.getStartDate() != null ? subscription.getStartDate().toString() : "",
                    "endDate", subscription.getEndDate() != null ? subscription.getEndDate().toString() : ""));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Force sync all subscriptions from SQLite to Supabase.
     * POST /api/subscriptions/fix-sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> fixSyncAll() {
        Map<String, Object> response = new HashMap<>();

        try {
            if (supabaseSyncService == null || !supabaseSyncService.isConfigured()) {
                response.put("success", false);
                response.put("error", "Supabase not configured");
                return ResponseEntity.status(500).body(response);
            }

            List<Subscription> subscriptions = subscriptionRepository.findAll();
            int syncedCount = 0;
            int failedCount = 0;

            System.out.println("========================================");
            System.out.println("FIXING SYNC for ALL subscriptions");
            System.out.println("Total subscriptions: " + subscriptions.size());
            System.out.println("========================================");

            for (Subscription subscription : subscriptions) {
                try {
                    System.out.println("Syncing subscription ID: " + subscription.getId() +
                            " (user: " + subscription.getUserId() +
                            ", status: " + subscription.getStatus() + ")");

                    // Use UPDATE to overwrite Supabase data with SQLite data
                    SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "UPDATE");
                    syncedCount++;
                } catch (Exception e) {
                    failedCount++;
                    System.err.println("Failed to sync subscription ID: " + subscription.getId() +
                            " - " + e.getMessage());
                }
            }

            System.out.println("========================================");
            System.out.println("Sync Complete!");
            System.out.println("  ✓ Successfully synced: " + syncedCount);
            if (failedCount > 0) {
                System.out.println("  ✗ Failed: " + failedCount);
            }
            System.out.println("========================================");

            response.put("success", true);
            response.put("syncedCount", syncedCount);
            response.put("failedCount", failedCount);
            response.put("totalSubscriptions", subscriptions.size());
            response.put("message", "Synced " + syncedCount + " subscription(s) to Supabase");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }
}
