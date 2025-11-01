package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", insertable = false, updatable = false)
    private Section section;

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====
    
    /**
     * Get the type of assignment (e.g., "INSTRUCTOR", "TA").
     * Subclasses must implement this to identify their role.
     * 
     * @return the assignment type
     */
    public abstract String getAssignmentType();
    
    /**
     * Check if this assignment is for the given user.
     * 
     * @param userId the user ID to check
     * @return true if assignment belongs to this user
     */
    public boolean belongsToUser(String userId) {
        return this.userId != null && this.userId.equals(userId);
    }
    
    /**
     * Check if this assignment is for the given section.
     * 
     * @param sectionId the section ID to check
     * @return true if assignment is for this section
     */
    public boolean isForSection(Long sectionId) {
        return this.sectionId != null && this.sectionId.equals(sectionId);
    }
    
    /**
     * Check if the assignment has a section loaded.
     * 
     * @return true if section relationship is loaded
     */
    public boolean hasSectionLoaded() {
        return section != null;
    }
    
    /**
     * Get the full assignment description.
     * Example: "TA assignment for CS102-001"
     * 
     * @return formatted description
     */
    public String getAssignmentDescription() {
        String sectionCode = (section != null && section.getSectionCode() != null) 
            ? section.getSectionCode() 
            : "Section " + sectionId;
        return getAssignmentType() + " assignment for " + sectionCode;
    }
}

