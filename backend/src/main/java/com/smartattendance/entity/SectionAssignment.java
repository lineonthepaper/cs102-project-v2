package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * Represents an instructor's assignment to a section.
 * Extends BaseAssignment to inherit common assignment functionality.
 * 
 * FIXED: Added @Getter and validated setters (ISP compliance)
 */
@Getter  // Generate getters
@Entity
@Table(name = "section_assignments")
public class SectionAssignment extends BaseAssignment {
    
    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "is_active")
    private Boolean isActive;

    public SectionAssignment() {
    }

    public SectionAssignment(String userId, Long sectionId, String role, Boolean isActive) {
        this.setUserId(userId);
        this.setSectionId(sectionId);
        this.role = role;
        this.isActive = isActive;
    }

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====
    
    @Override
    public String getAssignmentType() {
        return role != null ? role : "INSTRUCTOR";
    }
    
    /**
     * Check if this assignment is currently active.
     * 
     * @return true if assignment is active
     */
    public boolean isCurrentlyActive() {
        return Boolean.TRUE.equals(isActive);
    }
    
    /**
     * Activate this assignment.
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * Deactivate this assignment.
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * Check if this is an instructor role.
     * 
     * @return true if role is INSTRUCTOR
     */
    public boolean isInstructorRole() {
        return "INSTRUCTOR".equalsIgnoreCase(role);
    }
    
    /**
     * Check if assignment can be modified.
     * Business rule: Can only modify active assignments.
     * 
     * @return true if can be modified
     */
    public boolean canModify() {
        return isCurrentlyActive();
    }

    // ===== VALIDATED SETTERS (ISP Fix) =====
    
    public void setRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        String upperRole = role.toUpperCase();
        if (!upperRole.equals("INSTRUCTOR") && !upperRole.equals("TA")) {
            throw new IllegalArgumentException("Role must be INSTRUCTOR or TA, got: " + role);
        }
        this.role = upperRole;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }
}

