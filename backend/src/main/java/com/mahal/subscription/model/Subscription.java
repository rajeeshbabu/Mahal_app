package com.mahal.subscription.model;

import jakarta.persistence.*;
import com.mahal.util.LocalDateTimeConverter;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "plan_duration", nullable = false)
    private String planDuration; // "monthly" or "yearly"

    @Column(name = "status", nullable = false)
    private String status; // "pending", "active", "cancelled", "expired"

    @Column(name = "start_date")
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime endDate;

    @Column(name = "razorpay_subscription_id", unique = true)
    private String razorpaySubscriptionId;

    @Column(name = "created_at")
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;

    @Column(name = "superadmin_status")
    private String superadminStatus; // "activated", "deactivated"

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc();
        }
        if (updatedAt == null) {
            updatedAt = com.mahal.subscription.service.SubscriptionSyncHelper.getNowUtc();
        }
        // Default to activated if null
        if (this.superadminStatus == null) {
            this.superadminStatus = "activated";
        }
    }

    // Removed @PreUpdate to allow manual control of timestamps during sync

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPlanDuration() {
        return planDuration;
    }

    public void setPlanDuration(String planDuration) {
        this.planDuration = planDuration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSuperadminStatus() {
        return superadminStatus;
    }

    public void setSuperadminStatus(String superadminStatus) {
        this.superadminStatus = superadminStatus;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getRazorpaySubscriptionId() {
        return razorpaySubscriptionId;
    }

    public void setRazorpaySubscriptionId(String razorpaySubscriptionId) {
        this.razorpaySubscriptionId = razorpaySubscriptionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
