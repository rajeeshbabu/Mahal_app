package com.mahal.model;

import java.time.LocalDate;

public class Certificate {
    private Long id;
    private String type; // Marriage, Death, Jamath
    
    // Common fields
    private String certificateNo;
    private String name;
    private LocalDate issueDate;
    
    // Marriage-specific fields
    private String groomName;
    private String brideName;
    private String parentNameOfGroom;
    private String parentNameOfBride;
    private String addressOfGroom;
    private String addressOfBride;
    private String placeOfMarriage;
    private String marriageStatus;
    private LocalDate marriageDate;
    private String additionalNotes;
    
    // Death-specific fields
    private String parentName;
    private String address;
    private String thalook;
    private LocalDate dateOfDeath;
    private String cause;
    private String placeOfDeath;
    
    // Jamath-specific fields
    private String remarks;
    
    // Custom-specific fields
    private String templateName;
    private String templateContent;
    private String fieldData; // JSON string
    
    // File paths
    private String pdfPath;
    private String qrCode;
    private String supportingDocsPath;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCertificateNo() { return certificateNo; }
    public void setCertificateNo(String certificateNo) { this.certificateNo = certificateNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    // Marriage fields
    public String getGroomName() { return groomName; }
    public void setGroomName(String groomName) { this.groomName = groomName; }

    public String getBrideName() { return brideName; }
    public void setBrideName(String brideName) { this.brideName = brideName; }

    public String getParentNameOfGroom() { return parentNameOfGroom; }
    public void setParentNameOfGroom(String parentNameOfGroom) { this.parentNameOfGroom = parentNameOfGroom; }

    public String getParentNameOfBride() { return parentNameOfBride; }
    public void setParentNameOfBride(String parentNameOfBride) { this.parentNameOfBride = parentNameOfBride; }

    public String getAddressOfGroom() { return addressOfGroom; }
    public void setAddressOfGroom(String addressOfGroom) { this.addressOfGroom = addressOfGroom; }

    public String getAddressOfBride() { return addressOfBride; }
    public void setAddressOfBride(String addressOfBride) { this.addressOfBride = addressOfBride; }

    public String getPlaceOfMarriage() { return placeOfMarriage; }
    public void setPlaceOfMarriage(String placeOfMarriage) { this.placeOfMarriage = placeOfMarriage; }

    public String getMarriageStatus() { return marriageStatus; }
    public void setMarriageStatus(String marriageStatus) { this.marriageStatus = marriageStatus; }

    public LocalDate getMarriageDate() { return marriageDate; }
    public void setMarriageDate(LocalDate marriageDate) { this.marriageDate = marriageDate; }

    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }

    // Death fields
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getThalook() { return thalook; }
    public void setThalook(String thalook) { this.thalook = thalook; }

    public LocalDate getDateOfDeath() { return dateOfDeath; }
    public void setDateOfDeath(LocalDate dateOfDeath) { this.dateOfDeath = dateOfDeath; }

    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }

    public String getPlaceOfDeath() { return placeOfDeath; }
    public void setPlaceOfDeath(String placeOfDeath) { this.placeOfDeath = placeOfDeath; }

    // Jamath fields
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    // Custom fields
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }

    public String getFieldData() { return fieldData; }
    public void setFieldData(String fieldData) { this.fieldData = fieldData; }

    // File paths
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getSupportingDocsPath() { return supportingDocsPath; }
    public void setSupportingDocsPath(String supportingDocsPath) { this.supportingDocsPath = supportingDocsPath; }
    
    // Helper methods for display
    public String getDisplayName() {
        if ("Marriage".equals(type)) {
            return groomName != null ? groomName + " & " + (brideName != null ? brideName : "") : name;
        }
        return name != null ? name : "";
    }
    
    public String getReferenceNo() {
        return certificateNo;
    }
    
    public void setReferenceNo(String referenceNo) {
        this.certificateNo = referenceNo;
    }
}
