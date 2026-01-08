package com.mahal.subscription.repository;

import com.mahal.subscription.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndStatus(String userId, String status);

    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    Optional<Subscription> findTopByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Subscription> findTopByUserEmailOrderByCreatedAtDesc(String userEmail);

    @org.springframework.transaction.annotation.Transactional
    void deleteAllByUserIdNot(String userId);
}
