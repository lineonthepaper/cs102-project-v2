package com.smartattendance.dto.response;

import java.util.List;

public class InstructorDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private String authId;
    private List<SectionAssignmentDTO> sectionAssignments;

    public InstructorDTO() {
    }

    public InstructorDTO(String id, String email, String firstName, String lastName, 
                         Boolean enabled, String authId, List<SectionAssignmentDTO> sectionAssignments) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.authId = authId;
        this.sectionAssignments = sectionAssignments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public List<SectionAssignmentDTO> getSectionAssignments() {
        return sectionAssignments;
    }

    public void setSectionAssignments(List<SectionAssignmentDTO> sectionAssignments) {
        this.sectionAssignments = sectionAssignments;
    }
}

