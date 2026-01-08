package com.mahal.model;

import java.math.BigDecimal;

public class IncomeType {
    private Long id;
    private String name;
    private String type; // DONATION, RENT, ZAKAT, FEE, OTHER
    private BigDecimal defaultAmount;
    private String description;
    
    public IncomeType() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

