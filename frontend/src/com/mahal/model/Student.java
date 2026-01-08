package com.mahal.model;

import java.time.LocalDate;

public class Student {
    private Long id;
    private String name;
    private String course;
    private String admissionNumber;
    private LocalDate admissionDate;
    private String mobile;
    private String email;
    private String address;
    private String fatherName;
    private String motherName;
    private String guardianMobile;
    private String notes;
    
    public Student() {}
    
    public Student(Long id, String name, String course) {
        this.id = id;
        this.name = name;
        this.course = course;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getAdmissionNumber() { return admissionNumber; }
    public void setAdmissionNumber(String admissionNumber) { this.admissionNumber = admissionNumber; }
    
    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }
    
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }
    
    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }
    
    public String getGuardianMobile() { return guardianMobile; }
    public void setGuardianMobile(String guardianMobile) { this.guardianMobile = guardianMobile; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

