package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter 
@Entity
@NoArgsConstructor
@Table(name = "ta_assignments")
public class TAAssignment extends BaseAssignment {

    public TAAssignment(String userId, Long sectionId) {
        this.setUserId(userId);
        this.setSectionId(sectionId);
    }

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====
    
    @Override
    public String getAssignmentType() {
        return "TA";
    }
    
    /**
     * Check if this TA assignment allows managing attendance.
     * Business rule: TAs can always manage attendance for their assigned sections.
     * 
     * @return true (TAs can manage attendance)
     */
    public boolean canManageAttendance() {
        return true;
    }
    
    /**
     * Check if this TA assignment allows grading.
     * Business rule: Depends on course policy - for now, return true.
     * 
     * @return true if TA can grade
     */
    public boolean canGrade() {
        return true;
    }
    
    /**
     * Get the TA's responsibilities description.
     * 
     * @return description of TA responsibilities
     */
    public String getResponsibilities() {
        return "Teaching Assistant for section - Can manage attendance and assist with grading";
    }
}

