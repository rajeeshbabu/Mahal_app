package com.mahal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Staff {
    private Long id;
    private String name;
    private String designation;
    private BigDecimal salary;
    private String address;
    private String mobile;
    private String email;
    private LocalDate joiningDate;
    private String notes;
    
    public Staff() {}
    
    public Staff(Long id, String name, String position, String phone) {
        this.id = id;
        this.name = name;
        this.designation = position;
        this.mobile = phone;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    // For backward compatibility
    public String getPosition() { return designation; }
    public String getPhone() { return mobile; }
}

