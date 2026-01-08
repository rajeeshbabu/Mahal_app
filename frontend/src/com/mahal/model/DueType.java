package com.mahal.model;

import java.math.BigDecimal;

public class DueType {
    private Long id;
    private String dueName;
    private String frequency; // MONTHLY, ANNUAL, ONE_TIME
    private BigDecimal amount;
    private String description;
    
    public DueType() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDueName() { return dueName; }
    public void setDueName(String dueName) { this.dueName = dueName; }
    
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

