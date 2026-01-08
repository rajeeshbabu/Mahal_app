package com.mahal.model;

public class House {
    private Long id;
    private String address;
    private String houseNumber;
    
    public House() {}
    
    public House(Long id, String address, String houseNumber) {
        this.id = id;
        this.address = address;
        this.houseNumber = houseNumber;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }
    
    // Helper method to get display string for ComboBox
    public String getDisplayString() {
        if (houseNumber != null && !houseNumber.trim().isEmpty()) {
            return address + " (House No: " + houseNumber + ")";
        }
        return address;
    }
}


