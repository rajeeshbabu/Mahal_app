package com.mahal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Rent {
    private Long id;
    private Long rentItemId;
    private String rentItemName;
    private String renterName;
    private String renterMobile;
    private LocalDate rentStartDate;
    private LocalDate rentEndDate;
    private BigDecimal amount;
    private BigDecimal deposit;
    private String status; // BOOKED, RETURNED, OVERDUE

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRentItemId() { return rentItemId; }
    public void setRentItemId(Long rentItemId) { this.rentItemId = rentItemId; }

    public String getRentItemName() { return rentItemName; }
    public void setRentItemName(String rentItemName) { this.rentItemName = rentItemName; }

    public String getRenterName() { return renterName; }
    public void setRenterName(String renterName) { this.renterName = renterName; }

    public String getRenterMobile() { return renterMobile; }
    public void setRenterMobile(String renterMobile) { this.renterMobile = renterMobile; }

    public LocalDate getRentStartDate() { return rentStartDate; }
    public void setRentStartDate(LocalDate rentStartDate) { this.rentStartDate = rentStartDate; }

    public LocalDate getRentEndDate() { return rentEndDate; }
    public void setRentEndDate(LocalDate rentEndDate) { this.rentEndDate = rentEndDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public boolean isOverdue() {
        if (status == null || "RETURNED".equals(status)) return false;
        if (rentEndDate == null) return false;
        return rentEndDate.isBefore(LocalDate.now());
    }
}




