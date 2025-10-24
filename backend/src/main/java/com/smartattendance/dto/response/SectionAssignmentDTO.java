package com.smartattendance.dto.response;

public class SectionAssignmentDTO {
    private Long id;
    private String userId;
    private Long sectionId;
    private String role;
    private Boolean isActive;
    private SectionDTO section;

    public SectionAssignmentDTO() {
    }

    public SectionAssignmentDTO(Long id, String userId, Long sectionId, String role, Boolean isActive, SectionDTO section) {
        this.id = id;
        this.userId = userId;
        this.sectionId = sectionId;
        this.role = role;
        this.isActive = isActive;
        this.section = section;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public SectionDTO getSection() {
        return section;
    }

    public void setSection(SectionDTO section) {
        this.section = section;
    }
}

