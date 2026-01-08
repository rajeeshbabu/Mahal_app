package com.mahal.model;

import java.time.LocalDate;

public class Member {
    private Long id;
    private String name;
    private String qualification;
    private String fatherName;
    private String motherName;
    private String district;
    private String panchayat;
    private String mahal;
    private LocalDate dateOfBirth;
    private String address;
    private String mobile;
    private String gender;
    private String idProofType;
    private String idProofNo;
    private String photoPath;
    
    public Member() {}
    
    public Member(Long id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.mobile = phone;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    
    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }
    
    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }
    
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    
    public String getPanchayat() { return panchayat; }
    public void setPanchayat(String panchayat) { this.panchayat = panchayat; }
    
    public String getMahal() { return mahal; }
    public void setMahal(String mahal) { this.mahal = mahal; }
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getIdProofType() { return idProofType; }
    public void setIdProofType(String idProofType) { this.idProofType = idProofType; }
    
    public String getIdProofNo() { return idProofNo; }
    public void setIdProofNo(String idProofNo) { this.idProofNo = idProofNo; }
    
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    
    // For backward compatibility
    public String getEmail() { return ""; }
    public String getPhone() { return mobile; }
}

