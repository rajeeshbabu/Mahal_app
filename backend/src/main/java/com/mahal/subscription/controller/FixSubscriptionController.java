package com.mahal.subscription.controller;

import com.mahal.subscription.model.Subscription;
import com.mahal.subscription.repository.SubscriptionRepository;
import com.mahal.subscription.service.SubscriptionSyncHelper;
import com.mahal.sync.SupabaseSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for fixing subscription data issues.
 */
@RestController
@RequestMapping("/api/subscriptions/fix")
@CrossOrigin(origins = "*")
public class FixSubscriptionController {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired(required = false)
    private SupabaseSyncService supabaseSyncService;

    /**
     * Fix null end_date for a specific user's subscription.
     * POST /api/subscriptions/fix/end-date?userEmail=revu@gmail.com
     */
    @PostMapping("/end-date")
    public ResponseEntity<Map<String, Object>> fixEndDate(@RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String userId) {
        Map<String, Object> response = new HashMap<>();

        try {
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
            boolean updated = false;

            // Fix null end_date
            if (subscription.getEndDate() == null) {
                LocalDateTime startDate = subscription.getStartDate() != null
                        ? subscription.getStartDate()
                        : subscription.getCreatedAt() != null
                                ? subscription.getCreatedAt()
                                : com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc();

                if ("monthly".equals(subscription.getPlanDuration())) {
                    subscription.setEndDate(startDate.plusMonths(1));
                } else if ("yearly".equals(subscription.getPlanDuration())) {
                    subscription.setEndDate(startDate.plusYears(1));
                } else {
                    // Default to monthly
                    subscription.setEndDate(startDate.plusMonths(1));
                }

                // Also set start_date if it's null
                if (subscription.getStartDate() == null) {
                    subscription.setStartDate(startDate);
                }

                subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
                subscriptionRepository.save(subscription);
                updated = true;

                System.out.println("✅ Fixed end_date for subscription ID: " + subscription.getId());
                System.out.println("   User: " + subscription.getUserId());
                System.out.println("   Plan: " + subscription.getPlanDuration());
                System.out.println("   Start Date: " + subscription.getStartDate());
                System.out.println("   End Date: " + subscription.getEndDate());
            }

            // Sync to Supabase
            if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "UPDATE");
                System.out.println("✅ Synced fixed subscription to Supabase");
            }

            response.put("success", true);
            response.put("updated", updated);
            response.put("subscription", Map.of(
                    "id", subscription.getId(),
                    "userId", subscription.getUserId(),
                    "userEmail", subscription.getUserEmail() != null ? subscription.getUserEmail() : "",
                    "planDuration", subscription.getPlanDuration(),
                    "status", subscription.getStatus(),
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
     * Fix null end_date for all subscriptions.
     * POST /api/subscriptions/fix/end-date-all
     */
    @PostMapping("/end-date-all")
    public ResponseEntity<Map<String, Object>> fixAllEndDates() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Subscription> subscriptions = subscriptionRepository.findAll();
            int fixedCount = 0;

            for (Subscription subscription : subscriptions) {
                if (subscription.getEndDate() == null) {
                    LocalDateTime startDate = subscription.getStartDate() != null
                            ? subscription.getStartDate()
                            : subscription.getCreatedAt() != null
                                    ? subscription.getCreatedAt()
                                    : com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc();

                    if ("monthly".equals(subscription.getPlanDuration())) {
                        subscription.setEndDate(startDate.plusMonths(1));
                    } else if ("yearly".equals(subscription.getPlanDuration())) {
                        subscription.setEndDate(startDate.plusYears(1));
                    } else {
                        subscription.setEndDate(startDate.plusMonths(1));
                    }

                    if (subscription.getStartDate() == null) {
                        subscription.setStartDate(startDate);
                    }

                    subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
                    subscriptionRepository.save(subscription);
                    fixedCount++;

                    // Sync to Supabase
                    if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                        SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "UPDATE");
                    }
                }
            }

            response.put("success", true);
            response.put("fixedCount", fixedCount);
            response.put("totalSubscriptions", subscriptions.size());
            response.put("message", "Fixed " + fixedCount + " subscription(s) with null end_date");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Fix all subscription date formats by re-saving them.
     * This triggers the LocalDateTimeConverter to store them as IST strings.
     * POST /api/subscriptions/fix/format-alignment
     */
    @PostMapping("/format-alignment")
    public ResponseEntity<Map<String, Object>> fixFormatAlignment() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Subscription> subscriptions = subscriptionRepository.findAll();
            int migratedCount = 0;

            for (Subscription subscription : subscriptions) {
                // Simply re-saving the entity will trigger the LocalDateTimeConverter
                // and store the dates as strings in SQLite.
                subscriptionRepository.save(subscription);
                migratedCount++;
            }

            response.put("success", true);
            response.put("migratedCount", migratedCount);
            response.put("message", "Triggered format alignment for " + migratedCount
                    + " subscriptions. New timestamps will use IST string format.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }
}
