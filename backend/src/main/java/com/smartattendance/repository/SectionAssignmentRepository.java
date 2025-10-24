package com.smartattendance.repository;

import com.smartattendance.entity.SectionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionAssignmentRepository extends JpaRepository<SectionAssignment, Long> {
    List<SectionAssignment> findByUserIdAndRole(String userId, String role);
    List<SectionAssignment> findByUserIdAndRoleAndIsActive(String userId, String role, Boolean isActive);
    void deleteByUserIdAndRole(String userId, String role);
    
    /**
     * Check if any instructor assignments exist for a given section.
     * Used for dependency validation before section deletion.
     * 
     * @param sectionId the section ID to check
     * @return true if assignments exist, false otherwise
     */
    boolean existsBySectionId(Long sectionId);
    
    /**
     * Check if a user has any section assignments.
     * Used for dependency validation before user deletion.
     * 
     * @param userId the user ID to check
     * @return true if assignments exist, false otherwise
     */
    boolean existsByUserId(String userId);
}

