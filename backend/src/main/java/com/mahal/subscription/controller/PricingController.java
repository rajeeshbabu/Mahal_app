package com.mahal.subscription.controller;

import com.mahal.subscription.model.SubscriptionPricing;
import com.mahal.subscription.repository.SubscriptionPricingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = "*")
public class PricingController {

    @Autowired
    private SubscriptionPricingRepository pricingRepository;

    @Autowired(required = false)
    private com.mahal.sync.SupabaseSyncService supabaseSyncService;

    @GetMapping
    public List<SubscriptionPricing> getAllPricing() {
        // Try to sync with Supabase first to get latest prices set by admin via website
        syncFromSupabase();

        // Ensure default pricing exists if database is empty and sync failed/found
        // nothing
        if (pricingRepository.count() == 0) {
            initializeDefaults();
        }
        return pricingRepository.findAll();
    }

    private void syncFromSupabase() {
        if (supabaseSyncService != null && supabaseSyncService.isConfigured()) {
            try {
                System.out.println("🔄 Syncing subscription pricing from Supabase...");
                org.json.JSONArray pricingArray = supabaseSyncService.fetchAllPricing();

                if (pricingArray != null && pricingArray.length() > 0) {
                    for (int i = 0; i < pricingArray.length(); i++) {
                        org.json.JSONObject obj = pricingArray.getJSONObject(i);
                        String durationFromSupabase = obj.getString("plan_duration");
                        String duration = durationFromSupabase != null ? durationFromSupabase.toLowerCase() : "";
                        long amount = obj.getLong("amount_paise");

                        SubscriptionPricing pricing = pricingRepository.findByPlanDuration(duration)
                                .orElse(new SubscriptionPricing());

                        pricing.setPlanDuration(duration);
                        pricing.setAmountPaise(amount);
                        pricingRepository.save(pricing);
                    }
                    System.out
                            .println("✓ Successfully synced " + pricingArray.length() + " pricing plans from Supabase");
                }
            } catch (Exception e) {
                System.err.println("✗ Error syncing pricing from Supabase: " + e.getMessage());
            }
        }
    }

    @GetMapping("/{duration}")
    public ResponseEntity<SubscriptionPricing> getPricing(@PathVariable String duration) {
        Optional<SubscriptionPricing> pricing = pricingRepository.findByPlanDuration(duration);
        return pricing.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/update")
    public ResponseEntity<?> updatePricing(@RequestBody Map<String, Object> request) {
        String durationInput = (String) request.get("planDuration");
        String duration = durationInput != null ? durationInput.toLowerCase() : null;
        Long amount = Long.valueOf(request.get("amountPaise").toString());

        if (duration == null || (!duration.equals("monthly") && !duration.equals("yearly"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid plan duration"));
        }

        SubscriptionPricing pricing = pricingRepository.findByPlanDuration(duration)
                .orElse(new SubscriptionPricing());

        pricing.setPlanDuration(duration);
        pricing.setAmountPaise(amount);

        pricingRepository.save(pricing);

        return ResponseEntity.ok(Map.of("success", true, "pricing", pricing));
    }

    private void initializeDefaults() {
        SubscriptionPricing monthly = new SubscriptionPricing();
        monthly.setPlanDuration("monthly");
        monthly.setAmountPaise(100L); // ₹1.00
        pricingRepository.save(monthly);

        SubscriptionPricing yearly = new SubscriptionPricing();
        yearly.setPlanDuration("yearly");
        yearly.setAmountPaise(100L); // ₹1.00
        pricingRepository.save(yearly);
    }
}
