package com.mahal.subscription.service;

import com.mahal.subscription.dto.SubscriptionStatusResponse;
import com.mahal.subscription.model.Subscription;
import com.mahal.subscription.repository.SubscriptionRepository;

import com.mahal.sync.SupabaseSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired(required = false)
    private SupabaseSyncService supabaseSyncService;

    /**
     * Get subscription status for a user
     */
    public SubscriptionStatusResponse getSubscriptionStatus(String userIdentifier, String email) {
        SubscriptionStatusResponse response = new SubscriptionStatusResponse();

        try {
            // Log for debugging
            System.out.println("🔍 Investigating subscription status for userIdentifier: " + userIdentifier);

            // 1. Try to find by userId (numeric ID string)
            Optional<Subscription> subscriptionOpt = subscriptionRepository
                    .findTopByUserIdOrderByCreatedAtDesc(userIdentifier);

            // 2. Fallback: try to find by userEmail
            if (subscriptionOpt.isEmpty()) {
                System.out.println("   No subscription found for numeric ID, checking for email: " + userIdentifier);
                subscriptionOpt = subscriptionRepository.findTopByUserEmailOrderByCreatedAtDesc(userIdentifier);
            }

            if (subscriptionOpt.isPresent()) {
                Subscription subscription = subscriptionOpt.get();
                String userId = subscription.getUserId(); // Valid internal user ID

                // Bidirectional Sync (LWW based on timestamps + Status Override)
                if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                    try {
                        String userEmail = subscription.getUserEmail();
                        System.out.println(
                                "🔄 [SYNC] Comparing timestamps for user: " + userId + " (Email: " + userEmail + ")");

                        // Use email for more reliable lookups across local DB resets
                        org.json.JSONObject supabaseData = null;
                        if (userEmail != null && !userEmail.isEmpty()) {
                            supabaseData = supabaseSyncService.fetchSubscription(userEmail);
                        } else {
                            supabaseData = supabaseSyncService.fetchSubscription(userId);
                        }

                        if (supabaseData != null) {
                            // Verify that the remote record matches this user's email to avoid ID collision
                            String remoteEmail = supabaseData.optString("user_email", "");
                            if (userEmail != null && !userEmail.isEmpty() && !remoteEmail.isEmpty()
                                    && !userEmail.equalsIgnoreCase(remoteEmail)) {
                                System.err.println("⚠️ [SYNC] ID COLLISION DETECTED! Supabase record for ID " + userId
                                        + " belongs to " + remoteEmail + ", but local user is " + userEmail
                                        + ". Discarding remote data.");
                            } else {
                                // Create a temporary object to parse Supabase data for comparison
                                Subscription remoteSub = new Subscription();
                                SubscriptionSyncHelper.jsonToSubscription(supabaseData, remoteSub);

                                LocalDateTime localUpdated = subscription.getUpdatedAt() != null
                                        ? subscription.getUpdatedAt()
                                        : (subscription.getCreatedAt() != null ? subscription.getCreatedAt()
                                                : com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
                                LocalDateTime remoteUpdated = remoteSub.getUpdatedAt() != null
                                        ? remoteSub.getUpdatedAt()
                                        : (remoteSub.getCreatedAt() != null ? remoteSub.getCreatedAt()
                                                : com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());

                                System.out.println(
                                        "   [DEBUG] Local status: " + subscription.getStatus() + ", updated_at: "
                                                + localUpdated);
                                System.out
                                        .println("   [DEBUG] Remote status: " + remoteSub.getStatus() + ", updated_at: "
                                                + remoteUpdated);

                                // IMPROVED LOGIC: Prefer the Status from Supabase (Remote) if it differs.
                                // This ensures Super Admin deactivations (which happen in Supabase/Website)
                                // propagate to JavaFX immediately.
                                boolean statusMismatch = !remoteSub.getStatus()
                                        .equalsIgnoreCase(subscription.getStatus());

                                // IMPROVED SYNC LOGIC:
                                // 1. If Remote (Supabase) is strictly newer, PULL it.
                                // 2. If Local (SQLite) is strictly newer, PUSH it.
                                // 3. If timestamps are exactly the same but statuses differ, prefer Remote
                                // (Admin source of truth).

                                if (remoteUpdated.isAfter(localUpdated)) {
                                    System.out.println("⬇️ [SYNC] Supabase is newer (" + remoteUpdated + " > "
                                            + localUpdated + "). PULLING...");
                                    String oldStatus = subscription.getStatus();
                                    SubscriptionSyncHelper.jsonToSubscription(supabaseData, subscription);
                                    if (!oldStatus.equalsIgnoreCase(subscription.getStatus())) {
                                        System.out.println("⬇️ [SYNC] Status updated from " + oldStatus + " to "
                                                + subscription.getStatus());
                                    }
                                    subscription.setUpdatedAt(remoteUpdated);
                                    subscription = subscriptionRepository.save(subscription);
                                } else if (localUpdated.isAfter(remoteUpdated)) {
                                    System.out.println("⬆️ [SYNC] Local SQLite is newer (" + localUpdated + " > "
                                            + remoteUpdated + "). PUSHING...");
                                    SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription,
                                            "UPDATE");
                                } else if (statusMismatch) {
                                    System.out.println(
                                            "⬇️ [SYNC] Same timestamp but status mismatch. Preferring Remote (Admin override): "
                                                    + remoteSub.getStatus());
                                    SubscriptionSyncHelper.jsonToSubscription(supabaseData, subscription);
                                    subscription = subscriptionRepository.save(subscription);
                                } else {
                                    System.out.println("↔️ [SYNC] Both are in sync (Time: " + localUpdated + ")");
                                }
                            }
                        } else {
                            // Not in Supabase yet, push local record
                            System.out.println("⬆️ [SYNC] No record in Supabase. Pushing local record...");
                            SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");
                        }

                        // CLEANUP: Ensure only current user's data remains in local DB
                        try {
                            System.out.println("🧹 [CLEANUP] Removing other users' subscription data from local DB...");
                            subscriptionRepository.deleteAllByUserIdNot(userId);
                        } catch (Exception e) {
                        }

                    } catch (Exception syncEx) {
                        System.err.println("❌ [SYNC] Bidirectional sync failed: " + syncEx.getMessage());
                        syncEx.printStackTrace();
                    }
                }

                // Fallback cleanup
                try {
                    subscriptionRepository.deleteAllByUserIdNot(userId);
                } catch (Exception e) {
                }

                LocalDateTime now = com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc();

                // Log subscription details
                System.out.println("Found subscription: ID=" + subscription.getId() +
                        ", Status=" + subscription.getStatus() +
                        ", EndDate=" + subscription.getEndDate() +
                        ", Now=" + now);

                String currentStatus = subscription.getStatus() != null ? subscription.getStatus().trim() : "";
                boolean isActiveStatus = "active".equalsIgnoreCase(currentStatus);
                boolean isNotExpired = subscription.getEndDate() == null ||
                        !subscription.getEndDate().isBefore(now);

                System.out.println("🔍 [DEBUG] Evaluated status: '" + currentStatus + "', isActiveStatus: "
                        + isActiveStatus + ", isNotExpired: " + isNotExpired);

                // CRITICAL CHECK: Super Admin Status
                // If the Super Admin has explicitly deactivated this user, they cannot log in
                // regardless of their subscription status.
                String superAdminStatus = subscription.getSuperadminStatus();
                if ("deactivated".equalsIgnoreCase(superAdminStatus)) {
                    System.out.println(
                            "🚫 [GATEKEEPER] User is DEACTIVATED by Super Admin (Superadmin-Status: deactivated). Blocking access.");
                    isActiveStatus = false;
                    currentStatus = "deactivated_by_admin";

                    // Force response to match
                    response.setActive(false);
                    response.setStatus("account_deactivated");
                    response.setPlanDuration(subscription.getPlanDuration());
                    return response;
                }

                if (isActiveStatus && isNotExpired) {
                    // Subscription is active and valid
                    response.setActive(true);
                    response.setStatus("active");
                    response.setPlanDuration(subscription.getPlanDuration());
                    response.setEndDate(subscription.getEndDate());
                    System.out
                            .println("✅ [GATEKEEPER] Access GRANTED for user: " + userIdentifier + " (Status: ACTIVE)");
                } else if ("active".equalsIgnoreCase(currentStatus) && !isNotExpired) {
                    // Subscription status is active but expired
                    response.setActive(false);
                    response.setStatus("expired");
                    response.setPlanDuration(subscription.getPlanDuration());
                    response.setEndDate(subscription.getEndDate());
                    System.out.println(
                            "🚫 [GATEKEEPER] Access BLOCKED: Subscription EXPIRED for user: " + userIdentifier
                                    + " (End Date: " + subscription.getEndDate() + ")");
                } else if ("pending".equalsIgnoreCase(currentStatus)
                        || "created".equalsIgnoreCase(currentStatus)) {
                    // Subscription is pending/created
                    System.out.println("⏳ [SUBSYSTEM] Status is " + subscription.getStatus().toUpperCase()
                            + " for user: " + userIdentifier + ". Checking if we should check Razorpay...");

                    // If it's pending but has a Razorpay ID, try one last sync from Razorpay
                    String razorpaySubscriptionId = subscription.getRazorpaySubscriptionId();
                    if (razorpaySubscriptionId != null && !razorpaySubscriptionId.isEmpty()) {
                        System.out
                                .println("🔄 [SUBSYSTEM] Checking Razorpay fallback for ID: " + razorpaySubscriptionId);
                        boolean synced = syncSubscriptionStatusFromRazorpay(razorpaySubscriptionId, subscription);
                        if (synced) {
                            // Reload to get updated status
                            subscription = subscriptionRepository.findById(subscription.getId()).orElse(subscription);
                            if ("active".equalsIgnoreCase(subscription.getStatus())) {
                                response.setActive(true);
                                response.setStatus("active");
                                response.setPlanDuration(subscription.getPlanDuration());
                                response.setEndDate(subscription.getEndDate());
                                System.out.println("✅ [GATEKEEPER] Access GRANTED after Razorpay sync for user: "
                                        + userIdentifier);
                                return response;
                            }
                        }
                    }

                    response.setActive(false);
                    response.setStatus(subscription.getStatus());
                    response.setPlanDuration(subscription.getPlanDuration());
                    response.setEndDate(subscription.getEndDate());
                    System.out.println(
                            "🚫 [GATEKEEPER] Access BLOCKED: Subscription is PENDING for user: " + userIdentifier);
                } else {
                    // Subscription exists but not active (cancelled, expired, etc.)
                    response.setActive(false);
                    response.setStatus(currentStatus);
                    response.setPlanDuration(subscription.getPlanDuration());
                    response.setEndDate(subscription.getEndDate());
                    System.out.println(
                            "🚫 [GATEKEEPER] Access BLOCKED for user " + userIdentifier + ": Status="
                                    + currentStatus.toUpperCase());
                }
            } else {
                // No local subscription found - Check Supabase before giving up
                if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                    try {
                        System.out.println(
                                "🔍 [SUBSYSTEM] No local subscription found, checking Supabase for user: "
                                        + userIdentifier);

                        // Try both user_id and email format in Supabase if needed
                        // Prioritize the provided email if available to avoid ID collision
                        String searchKey = (email != null && !email.isEmpty()) ? email : userIdentifier;
                        System.out.println("🔍 [SUBSYSTEM] Using search key for Supabase: " + searchKey);
                        org.json.JSONObject supabaseData = supabaseSyncService.fetchSubscription(searchKey);

                        if (supabaseData != null) {
                            // Verify email match to avoid ID collision
                            String remoteEmail = supabaseData.optString("user_email", "").trim();
                            String validationEmail = (email != null && !email.isEmpty()) ? email
                                    : (userIdentifier.contains("@") ? userIdentifier : "");

                            if (!validationEmail.isEmpty() && !validationEmail.equalsIgnoreCase(remoteEmail)) {
                                System.err.println(
                                        "⚠️ [SUBSYSTEM] ID COLLISION PREVENTED! Supabase record for key " + searchKey +
                                                " belongs to " + remoteEmail + ", but we expected " + validationEmail +
                                                ". Discarding remote data.");
                                supabaseData = null;
                            } else {
                                System.out.println(
                                        "✅ [SUBSYSTEM] Found subscription in Supabase! Creating local record...");
                                Subscription newSub = new Subscription();
                                SubscriptionSyncHelper.jsonToSubscription(supabaseData, newSub);

                                // Ensure userId is correctly set from Supabase data if missing
                                if (newSub.getUserId() == null || newSub.getUserId().isEmpty()
                                        || "null".equals(newSub.getUserId())) {
                                    newSub.setUserId(userIdentifier);
                                }

                                newSub.setCreatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
                                newSub.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
                                newSub = subscriptionRepository.save(newSub);

                                String finalStatus = newSub.getStatus() != null ? newSub.getStatus().trim() : "";
                                response.setActive("active".equalsIgnoreCase(finalStatus));
                                response.setStatus(finalStatus);
                                response.setPlanDuration(newSub.getPlanDuration());
                                response.setEndDate(newSub.getEndDate());
                                System.out.println("✅ [SUBSYSTEM] Synced from Supabase. Status: " + finalStatus);
                                return response;
                            }
                        }
                    } catch (Exception syncEx) {
                        System.err.println("❌ [SUBSYSTEM] Supabase check failed: " + syncEx.getMessage());
                    }
                }

                // No subscription found for this user anywhere
                response.setActive(false);
                response.setStatus("not_found");
                System.out.println("⚠️ [SUBSYSTEM] No subscription found for user: " + userIdentifier);
            }
        } catch (Exception e) {
            // Error checking subscription - log and return error status
            System.err
                    .println("❌ Error checking subscription status for user " + userIdentifier + ": " + e.getMessage());
            System.err.println("❌ Exception type: " + e.getClass().getName());
            e.printStackTrace();
            response.setActive(false);
            response.setStatus("error");
            response.setPlanDuration(null);
            response.setEndDate(null);
        }

        return response;
    }

    /**
     * Create a pending subscription for a new user if one doesn't exist
     */
    public Subscription createPendingSubscription(String userId, String email) {
        // Check if subscription already exists
        Optional<Subscription> existingOpt = subscriptionRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId);

        if (existingOpt.isPresent()) {
            return existingOpt.get(); // Return existing subscription
        }

        // Create new pending subscription
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setUserEmail(email);
        subscription.setStatus("pending");
        subscription.setPlanDuration("monthly"); // Default to monthly
        subscription.setStartDate(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
        subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
        // No end date for pending subscription

        subscription = subscriptionRepository.save(subscription);
        System.out
                .println("✅ Created PENDING subscription for user: " + userId + " (ID: " + subscription.getId() + ")");

        // Sync to Supabase immediately even if pending
        if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
            System.out.println("🔄 Syncing PENDING subscription to Supabase...");
            System.out.println("   User ID: " + subscription.getUserId());
            System.out.println("   Status: " + subscription.getStatus());
            // Use INSERT to create record in Supabase (UPSERT)
            SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");
        } else {
            System.err.println("⚠️  Supabase not configured - pending subscription created in SQLite only");
        }

        return subscription;
    }

    /**
     * Get detailed subscription information
     */
    public Map<String, Object> getSubscriptionDetails(String userId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> details = new HashMap<>();

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            details.put("subscriptionId", subscription.getId());
            details.put("planDuration", subscription.getPlanDuration());
            details.put("status", subscription.getStatus());
            details.put("startDate", subscription.getStartDate());
            details.put("endDate", subscription.getEndDate());
            details.put("razorpaySubscriptionId", subscription.getRazorpaySubscriptionId());
        } else {
            details.put("status", "not_found");
        }

        return details;
    }

    /**
     * Activate subscription (called from webhook)
     */
    public void activateSubscription(String razorpaySubscriptionId, String planDuration) {
        System.out.println("🔄 [ACTIVATION] Starting subscription activation process");
        System.out.println("   Razorpay Subscription ID: " + razorpaySubscriptionId);
        System.out.println("   Plan Duration: " + planDuration);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByRazorpaySubscriptionId(razorpaySubscriptionId);

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            System.out.println("   Found subscription in database:");
            System.out.println("      DB ID: " + subscription.getId());
            System.out.println("      User ID: " + subscription.getUserId());
            System.out.println("      User Email: " + subscription.getUserEmail());
            System.out.println("      Current Status: " + subscription.getStatus());

            subscription.setStatus("active");

            // CRITICAL: Reset Super Admin Status to 'activated' on new payment
            subscription.setSuperadminStatus("activated");

            subscription.setStartDate(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
            subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());

            // Calculate end date based on plan duration
            if ("monthly".equals(planDuration)) {
                subscription
                        .setEndDate(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc().plusMonths(1));
            } else {
                subscription.setEndDate(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc().plusYears(1));
            }

            subscription = subscriptionRepository.save(subscription);

            System.out.println("✅ [ACTIVATION] Subscription activated successfully in database");
            System.out.println("   New Status: " + subscription.getStatus());
            System.out.println("   Start Date: " + subscription.getStartDate());
            System.out.println("   End Date: " + subscription.getEndDate());

            // Sync to Supabase only when status becomes "active"
            if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                System.out.println("🔄 Syncing subscription activation to Supabase...");
                System.out.println("   Status: " + subscription.getStatus());
                System.out.println("   Start Date: " + subscription.getStartDate());
                System.out.println("   End Date: " + subscription.getEndDate());
                System.out.println("   Razorpay ID: " + subscription.getRazorpaySubscriptionId());
                System.out.println("   User ID: " + subscription.getUserId());
                // Use INSERT to create record in Supabase (UPSERT - will update if exists)
                SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");
                System.out.println("✅ [ACTIVATION] Synced to Supabase successfully");
            } else {
                System.err.println("⚠️  Supabase not configured - subscription updated in SQLite only");
            }
        } else {
            System.err.println("❌ [ACTIVATION] Subscription not found for Razorpay ID: " + razorpaySubscriptionId);
            System.err.println("   This could mean:");
            System.err.println("   1. The subscription was never created in the database");
            System.err.println("   2. The Razorpay subscription ID doesn't match");
            System.err.println("   3. The subscription was deleted");
        }
    }

    /**
     * Cancel subscription (called from webhook)
     */
    public void cancelSubscription(String razorpaySubscriptionId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByRazorpaySubscriptionId(razorpaySubscriptionId);

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus("cancelled");
            subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
            subscription = subscriptionRepository.save(subscription);

            // Sync cancelled status to Supabase (broadened sync)
            if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                System.out.println("🔄 Syncing subscription cancellation to Supabase...");
                SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "UPDATE");
            }
        }
    }

    /**
     * Update subscription on renewal (called from webhook)
     */
    public void renewSubscription(String razorpaySubscriptionId, String planDuration) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByRazorpaySubscriptionId(razorpaySubscriptionId);

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus("active");
            subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());

            // Extend end date
            LocalDateTime currentEndDate = subscription.getEndDate() != null
                    ? subscription.getEndDate()
                    : com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc();

            if ("monthly".equals(planDuration)) {
                subscription.setEndDate(currentEndDate.plusMonths(1));
            } else {
                subscription.setEndDate(currentEndDate.plusYears(1));
            }

            subscription = subscriptionRepository.save(subscription);

            // Sync to Supabase only when status is "active" (renewal keeps status as
            // active)
            if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                System.out.println("🔄 Syncing subscription renewal to Supabase...");
                System.out.println("   Status: " + subscription.getStatus());
                System.out.println("   End Date: " + subscription.getEndDate());
                // Use INSERT to create/update record in Supabase (UPSERT)
                SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");
            } else {
                System.err.println("⚠️  Supabase not configured - subscription updated in SQLite only");
            }
        }
    }

    /**
     * Sync subscription status from Razorpay API (fallback when webhook hasn't
     * fired)
     */
    private boolean syncSubscriptionStatusFromRazorpay(String razorpaySubscriptionId, Subscription subscription) {
        // Obsolete: Manual Razorpay sync from backend is disabled in favor of Supabase
        // Edge Function proxy.
        // Status is now synced via supabaseSyncService.
        System.out.println(
                "ℹ️ Skipping manual Razorpay sync for " + razorpaySubscriptionId + " (handled via Supabase Proxy)");
        return false;
    }

    /**
     * Helper to auto-migrate schema by checking if column exists (Naive approach
     * for SQLite)
     * Real JDBC check would be better, but this ensures field is initialized.
     */
    @jakarta.annotation.PostConstruct
    public void initSchema() {
        try {
            // This is a bit hacky but for SQLite strict mode it's hard.
            // Ideally use Flyway or just let JPA Create it if ddl-auto=update
            System.out.println("ℹ️ [SCHEMA] Ensuring schema compatibility...");
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Update subscription status (used by webhooks and mock payment controller)
     */
    public void updateSubscriptionStatus(String razorpaySubscriptionId, String status, LocalDateTime endDate) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByRazorpaySubscriptionId(razorpaySubscriptionId);

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus(status);

            // CRITICAL: Reset Super Admin Status to 'activated' if activating
            if ("active".equalsIgnoreCase(status)) {
                subscription.setSuperadminStatus("activated");
            }

            // If activating subscription, set start date and end date if not already set
            if ("active".equalsIgnoreCase(status)) {
                if (subscription.getStartDate() == null) {
                    subscription.setStartDate(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
                }

                // If endDate is provided, use it. Otherwise, calculate based on plan duration
                if (endDate != null) {
                    subscription.setEndDate(endDate);
                } else if (subscription.getEndDate() == null) {
                    // Calculate end date based on plan duration
                    LocalDateTime startDate = subscription.getStartDate() != null
                            ? subscription.getStartDate()
                            : com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc();

                    if ("monthly".equals(subscription.getPlanDuration())) {
                        subscription.setEndDate(startDate.plusMonths(1));
                    } else if ("yearly".equals(subscription.getPlanDuration())) {
                        subscription.setEndDate(startDate.plusYears(1));
                    } else {
                        // Default to monthly
                        subscription.setEndDate(startDate.plusMonths(1));
                    }
                }
            } else {
                // For non-active status, set endDate if provided
                if (endDate != null) {
                    subscription.setEndDate(endDate);
                }
            }

            subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
            subscription = subscriptionRepository.save(subscription);

            // Sync all status changes to Supabase
            if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                System.out.println("🔄 Syncing subscription status change to Supabase...");
                System.out.println("   Razorpay ID: " + razorpaySubscriptionId);
                System.out.println("   Status: " + status);
                System.out.println("   End Date: " + subscription.getEndDate());
                System.out.println("   User ID: " + subscription.getUserId());
                // Use INSERT to create/update record in Supabase (UPSERT)
                SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "UPDATE");
            } else {
                System.err.println("⚠️  Supabase not configured - subscription updated in SQLite only");
            }

            // Log for debugging
            System.out.println("✅ Subscription updated in SQLite: ID=" + razorpaySubscriptionId +
                    ", Status=" + status +
                    ", UserId=" + subscription.getUserId() +
                    ", EndDate=" + subscription.getEndDate());
        } else {
            System.err.println("❌ Warning: Subscription not found for Razorpay ID: " + razorpaySubscriptionId);
        }
    }

    /**
     * Delete all subscriptions (for testing/reset purposes)
     * WARNING: This will delete ALL subscription records!
     */
    public long deleteAllSubscriptions() {
        long count = subscriptionRepository.count();
        subscriptionRepository.deleteAll();
        System.out.println("🗑️ Deleted " + count + " subscription(s) from database");
        return count;
    }

    /**
     * Activate subscription by User ID (fallback for Payment Links where
     * Subscription ID is missing)
     */
    public void activateSubscriptionForUser(String userId, String planDuration) {
        System.out.println("🔄 [ACTIVATION] Starting subscription activation process for User: " + userId);
        System.out.println("   Plan Duration: " + planDuration);

        // Find the latest pending or created subscription for this user
        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId);

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            System.out.println("   Found subscription in database:");
            System.out.println("      DB ID: " + subscription.getId());
            System.out.println("      Current Status: " + subscription.getStatus());

            // Activate subscription
            subscription.setStatus("active");
            // CRITICAL: Reset Super Admin Status to 'activated' on activation
            subscription.setSuperadminStatus("activated");
            subscription.setStartDate(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());

            // Calculate end date based on plan duration
            LocalDateTime startDate = subscription.getStartDate();
            if ("monthly".equals(planDuration)) {
                subscription.setEndDate(startDate.plusMonths(1));
            } else {
                subscription.setEndDate(startDate.plusYears(1));
            }

            subscription.setUpdatedAt(com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc());
            subscription = subscriptionRepository.save(subscription);

            System.out.println("✅ [ACTIVATION] Subscription activated successfully for User: " + userId);
            System.out.println("   New Status: " + subscription.getStatus());
            System.out.println("   End Date: " + subscription.getEndDate());

            // Sync to Supabase
            if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
                System.out.println("🔄 [ACTIVATION] Syncing to Supabase...");
                SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");
            }
        } else {
            System.err.println("❌ [ACTIVATION] No subscription record found for User: " + userId);
        }
    }
}
