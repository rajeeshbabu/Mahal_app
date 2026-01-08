package com.mahal.model;

import java.time.LocalDate;

public class DamagedItem {
    private Long id;
    private Long inventoryItemId;
    private String inventoryItemName;
    private Integer quantity;
    private LocalDate damageDate;
    private String reason;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(Long inventoryItemId) { this.inventoryItemId = inventoryItemId; }

    public String getInventoryItemName() { return inventoryItemName; }
    public void setInventoryItemName(String inventoryItemName) { this.inventoryItemName = inventoryItemName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDate getDamageDate() { return damageDate; }
    public void setDamageDate(LocalDate damageDate) { this.damageDate = damageDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}




