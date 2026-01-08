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

/**
 * Test controller for debugging Supabase sync.
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestSyncController {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired(required = false)
    private SupabaseSyncService supabaseSyncService;

    /**
     * Test Supabase configuration and list all subscriptions.
     */
    @GetMapping("/subscriptions/list")
    public ResponseEntity<Map<String, Object>> listSubscriptions() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Subscription> subscriptions = subscriptionRepository.findAll();
            
            response.put("success", true);
            response.put("count", subscriptions.size());
            response.put("subscriptions", subscriptions);
            response.put("supabaseConfigured", supabaseSyncService != null && supabaseSyncService.isConfigured());
            
            if (supabaseSyncService != null) {
                response.put("supabaseUrl", supabaseSyncService.getSupabaseUrl());
                response.put("supabaseKeySet", supabaseSyncService.getSupabaseApiKey() != null && !supabaseSyncService.getSupabaseApiKey().isEmpty());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Test syncing a single subscription with detailed logging.
     */
    @PostMapping("/subscriptions/sync/{id}")
    public ResponseEntity<Map<String, Object>> testSyncSubscription(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (supabaseSyncService == null || !supabaseSyncService.isConfigured()) {
                response.put("success", false);
                response.put("error", "Supabase not configured");
                response.put("supabaseUrl", supabaseSyncService != null ? supabaseSyncService.getSupabaseUrl() : "null");
                return ResponseEntity.status(500).body(response);
            }

            Subscription subscription = subscriptionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));

            System.out.println("========================================");
            System.out.println("TEST SYNC - Subscription ID: " + id);
            System.out.println("========================================");
            System.out.println("Subscription Details:");
            System.out.println("  ID: " + subscription.getId());
            System.out.println("  User ID: " + subscription.getUserId());
            System.out.println("  User Email: " + subscription.getUserEmail());
            System.out.println("  Status: " + subscription.getStatus());
            System.out.println("  Plan Duration: " + subscription.getPlanDuration());
            System.out.println("  Razorpay ID: " + subscription.getRazorpaySubscriptionId());
            System.out.println("  Start Date: " + subscription.getStartDate());
            System.out.println("  End Date: " + subscription.getEndDate());
            System.out.println("========================================");

            // Convert to JSON and print
            String jsonData = SubscriptionSyncHelper.subscriptionToJson(subscription);
            System.out.println("JSON Data:");
            System.out.println(jsonData);
            System.out.println("========================================");

            // Try to sync
            SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");

            response.put("success", true);
            response.put("message", "Sync attempted. Check backend logs for details.");
            response.put("subscriptionId", id);
            response.put("jsonData", jsonData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }
}

