package com.mahal.subscription.repository;

import com.mahal.subscription.model.SubscriptionPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPricingRepository extends JpaRepository<SubscriptionPricing, Long> {
    Optional<SubscriptionPricing> findByPlanDuration(String planDuration);
}
