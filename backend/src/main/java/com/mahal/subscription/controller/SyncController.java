package com.mahal.subscription.controller;

import com.mahal.subscription.service.SubscriptionSyncService;
import com.mahal.sync.BidirectionalSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for manually syncing subscriptions to Supabase and pulling from Supabase.
 */
@RestController
@RequestMapping("/api/subscriptions/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    @Autowired
    private SubscriptionSyncService subscriptionSyncService;

    @Autowired(required = false)
    private BidirectionalSyncService bidirectionalSyncService;

    /**
     * Sync all existing subscriptions from H2 database to Supabase.
     * GET /api/subscriptions/sync/all
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAllSubscriptions() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int syncedCount = subscriptionSyncService.syncAllSubscriptionsToSupabase();
            response.put("success", true);
            response.put("message", "Synced " + syncedCount + " subscriptions to Supabase");
            response.put("syncedCount", syncedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error syncing subscriptions: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Sync a specific subscription by ID.
     * GET /api/subscriptions/sync/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> syncSubscriptionById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = subscriptionSyncService.syncSubscriptionById(id);
            if (success) {
                response.put("success", true);
                response.put("message", "Subscription " + id + " synced successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Subscription " + id + " not found or sync failed");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error syncing subscription: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Pull subscriptions from Supabase to local database.
     * POST /api/subscriptions/sync/pull-from-supabase
     */
    @PostMapping("/pull-from-supabase")
    public ResponseEntity<Map<String, Object>> pullFromSupabase() {
        Map<String, Object> response = new HashMap<>();
        try {
            if (bidirectionalSyncService == null) {
                response.put("success", false);
                response.put("message", "Bidirectional sync service not available");
                return ResponseEntity.status(500).body(response);
            }
            
            bidirectionalSyncService.pullFromSupabase();
            response.put("success", true);
            response.put("message", "Successfully pulled subscriptions from Supabase");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error pulling from Supabase: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}


