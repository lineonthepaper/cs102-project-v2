package com.smartattendance.repository;

import com.smartattendance.entity.TAAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TAAssignmentRepository extends JpaRepository<TAAssignment, Long> {
    List<TAAssignment> findByUserId(String userId);
    void deleteByUserId(String userId);
    
    /**
     * Check if any TA assignments exist for a given section.
     * Used for dependency validation before section deletion.
     * 
     * @param sectionId the section ID to check
     * @return true if assignments exist, false otherwise
     */
    boolean existsBySectionId(Long sectionId);
    
    /**
     * Check if a user has any TA assignments.
     * Used for dependency validation before user deletion.
     * 
     * @param userId the user ID to check
     * @return true if assignments exist, false otherwise
     */
    boolean existsByUserId(String userId);
}

