package com.smartattendance.dto.response;

import java.util.List;

public class TADTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isStudent;
    private Boolean isInstructor;
    private Boolean isTA;
    private Boolean enabled;
    private String authId;
    private List<TAAssignmentDTO> taAssignments;

    public TADTO() {
    }

    public TADTO(String id, String email, String firstName, String lastName, Boolean isStudent, 
                 Boolean isInstructor, Boolean isTA, Boolean enabled, String authId, List<TAAssignmentDTO> taAssignments) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.isStudent = isStudent;
        this.isInstructor = isInstructor;
        this.isTA = isTA;
        this.enabled = enabled;
        this.authId = authId;
        this.taAssignments = taAssignments;
    }

    // Getters and setters
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

    public Boolean getIsStudent() {
        return isStudent;
    }

    public void setIsStudent(Boolean isStudent) {
        this.isStudent = isStudent;
    }

    public Boolean getIsInstructor() {
        return isInstructor;
    }

    public void setIsInstructor(Boolean isInstructor) {
        this.isInstructor = isInstructor;
    }

    public Boolean getIsTA() {
        return isTA;
    }

    public void setIsTA(Boolean isTA) {
        this.isTA = isTA;
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

    public List<TAAssignmentDTO> getTaAssignments() {
        return taAssignments;
    }

    public void setTaAssignments(List<TAAssignmentDTO> taAssignments) {
        this.taAssignments = taAssignments;
    }
}

