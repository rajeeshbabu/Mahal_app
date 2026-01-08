package com.mahal.model;

import java.time.LocalDateTime;

public class Event {
    private Long id;
    private String eventName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String eventPlace;
    private Long masjidId;
    private String masjidName;
    private String eventDetails;
    private String organizer;
    private String contact;
    private String attachmentsPath;
    private Boolean isPublic;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public String getEventPlace() { return eventPlace; }
    public void setEventPlace(String eventPlace) { this.eventPlace = eventPlace; }

    public Long getMasjidId() { return masjidId; }
    public void setMasjidId(Long masjidId) { this.masjidId = masjidId; }

    public String getMasjidName() { return masjidName; }
    public void setMasjidName(String masjidName) { this.masjidName = masjidName; }

    public String getEventDetails() { return eventDetails; }
    public void setEventDetails(String eventDetails) { this.eventDetails = eventDetails; }

    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getAttachmentsPath() { return attachmentsPath; }
    public void setAttachmentsPath(String attachmentsPath) { this.attachmentsPath = attachmentsPath; }

    public Boolean getIsPublic() { return isPublic != null ? isPublic : true; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}




