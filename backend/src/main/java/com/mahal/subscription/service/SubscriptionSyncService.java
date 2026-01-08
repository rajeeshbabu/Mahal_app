package com.mahal.subscription.service;

import com.mahal.subscription.model.Subscription;
import com.mahal.subscription.repository.SubscriptionRepository;
import com.mahal.sync.SupabaseSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to sync existing subscriptions to Supabase.
 * Run this manually via a REST endpoint or main method to sync existing
 * subscriptions.
 */
@Service
public class SubscriptionSyncService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired(required = false)
    private SupabaseSyncService supabaseSyncService;

    /**
     * Sync all existing subscriptions from H2 database to Supabase.
     * Returns the number of subscriptions synced.
     */
    public int syncAllSubscriptionsToSupabase() {
        if (supabaseSyncService == null || !supabaseSyncService.isConfigured()) {
            System.err.println("⚠️  Supabase not configured. Cannot sync subscriptions.");
            System.err.println("   Please configure supabase.url and supabase.key in application.properties");
            return 0;
        }

        List<Subscription> subscriptions = subscriptionRepository.findAll();
        int syncedCount = 0;
        int failedCount = 0;

        System.out.println("========================================");
        System.out.println("Syncing " + subscriptions.size() + " subscriptions to Supabase...");
        System.out.println("========================================");

        for (Subscription subscription : subscriptions) {
            try {
                SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");
                syncedCount++;
                System.out.println("✓ Synced subscription ID: " + subscription.getId() +
                        " (user: " + subscription.getUserId() + ")");
            } catch (Exception e) {
                failedCount++;
                System.err.println("✗ Failed to sync subscription ID: " + subscription.getId() +
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

        return syncedCount;
    }

    /**
     * Sync a specific subscription by ID.
     */
    public boolean syncSubscriptionById(Long subscriptionId) {
        if (supabaseSyncService == null || !supabaseSyncService.isConfigured()) {
            System.err.println("⚠️  Supabase not configured.");
            return false;
        }

        return subscriptionRepository.findById(subscriptionId)
                .map(subscription -> {
                    try {
                        SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");
                        System.out.println("✓ Synced subscription ID: " + subscriptionId);
                        return true;
                    } catch (Exception e) {
                        System.err.println("✗ Failed to sync subscription ID: " + subscriptionId +
                                " - " + e.getMessage());
                        return false;
                    }
                })
                .orElse(false);
    }
}
