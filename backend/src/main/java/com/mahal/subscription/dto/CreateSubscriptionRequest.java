package com.mahal.subscription.dto;

public class CreateSubscriptionRequest {
    private String planDuration; // "monthly" or "yearly"

    public String getPlanDuration() {
        return planDuration;
    }

    public void setPlanDuration(String planDuration) {
        this.planDuration = planDuration;
    }
}

