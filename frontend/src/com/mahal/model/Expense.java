package com.mahal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Expense {
    private Long id;
    private String expenseType;
    private BigDecimal amount;
    private LocalDate date;
    private Long masjidId;
    private String masjidName;
    private String notes;
    private String receiptPath;
    
    public Expense() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public Long getMasjidId() { return masjidId; }
    public void setMasjidId(Long masjidId) { this.masjidId = masjidId; }
    
    public String getMasjidName() { return masjidName; }
    public void setMasjidName(String masjidName) { this.masjidName = masjidName; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }
}

