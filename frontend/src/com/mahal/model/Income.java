package com.mahal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Income {
    private Long id;
    private Long masjidId;
    private String masjidName;
    private Long memberId;
    private String memberName;
    private Long incomeTypeId;
    private String incomeTypeName;
    private BigDecimal amount;
    private LocalDate date;
    private String paymentMode;
    private String receiptNo;
    private String remarks;
    
    public Income() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getMasjidId() { return masjidId; }
    public void setMasjidId(Long masjidId) { this.masjidId = masjidId; }
    
    public String getMasjidName() { return masjidName; }
    public void setMasjidName(String masjidName) { this.masjidName = masjidName; }
    
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    
    public Long getIncomeTypeId() { return incomeTypeId; }
    public void setIncomeTypeId(Long incomeTypeId) { this.incomeTypeId = incomeTypeId; }
    
    public String getIncomeTypeName() { return incomeTypeName; }
    public void setIncomeTypeName(String incomeTypeName) { this.incomeTypeName = incomeTypeName; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}

