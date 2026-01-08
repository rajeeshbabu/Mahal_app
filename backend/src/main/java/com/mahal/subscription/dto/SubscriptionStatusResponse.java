package com.mahal.subscription.dto;

import java.time.LocalDateTime;

public class SubscriptionStatusResponse {
    private boolean active;
    private String status; // "active", "expired", "not_found"
    private String planDuration; // "monthly" or "yearly"
    private LocalDateTime endDate;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlanDuration() {
        return planDuration;
    }

    public void setPlanDuration(String planDuration) {
        this.planDuration = planDuration;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
