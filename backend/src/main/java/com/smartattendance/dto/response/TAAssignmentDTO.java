package com.smartattendance.dto.response;

public class TAAssignmentDTO {
    private Long id;
    private String userId;
    private Long sectionId;
    private SectionDTO section;

    public TAAssignmentDTO() {
    }

    public TAAssignmentDTO(Long id, String userId, Long sectionId, SectionDTO section) {
        this.id = id;
        this.userId = userId;
        this.sectionId = sectionId;
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

    public SectionDTO getSection() {
        return section;
    }

    public void setSection(SectionDTO section) {
        this.section = section;
    }
}

