package com.mahal.subscription.model;

import jakarta.persistence.*;
import com.mahal.util.LocalDateTimeConverter;
import java.time.LocalDateTime;
import jakarta.persistence.Convert;

@Entity
@Table(name = "subscription_pricing")
public class SubscriptionPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_duration", nullable = false, unique = true)
    private String planDuration; // "monthly" or "yearly"

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "currency")
    private String currency = "INR";

    @Column(name = "updated_at")
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlanDuration() {
        return planDuration;
    }

    public void setPlanDuration(String planDuration) {
        this.planDuration = planDuration;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
