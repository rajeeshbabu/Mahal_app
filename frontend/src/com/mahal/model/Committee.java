package com.mahal.model;

public class Committee {
    private Long id;
    private String memberName;
    private String mobile;
    private String designation;
    private String otherDetails;
    private Long masjidId;
    private String masjidName;
    
    public Committee() {}
    
    public Committee(Long id, String memberName, String mobile, String designation, Long masjidId) {
        this.id = id;
        this.memberName = memberName;
        this.mobile = mobile;
        this.designation = designation;
        this.masjidId = masjidId;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    
    public String getOtherDetails() { return otherDetails; }
    public void setOtherDetails(String otherDetails) { this.otherDetails = otherDetails; }
    
    public Long getMasjidId() { return masjidId; }
    public void setMasjidId(Long masjidId) { this.masjidId = masjidId; }
    
    public String getMasjidName() { return masjidName; }
    public void setMasjidName(String masjidName) { this.masjidName = masjidName; }
}

