package com.smartattendance.repository;

import com.smartattendance.entity.SectionEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SectionEnrollmentRepository extends JpaRepository<SectionEnrollment, Long> {
    List<SectionEnrollment> findByUserIdAndIsActive(String userId, Boolean isActive);
    List<SectionEnrollment> findByUserIdInAndIsActive(List<String> userIds, Boolean isActive);
    List<SectionEnrollment> findBySectionIdAndIsActive(Long sectionId, Boolean isActive);
    Optional<SectionEnrollment> findByUserIdAndSectionId(String userId, Long sectionId);
    
    /**
     * Check if any enrollments exist for a given section.
     * Used for dependency validation before section deletion.
     * 
     * @param sectionId the section ID to check
     * @return true if enrollments exist, false otherwise
     */
    boolean existsBySectionId(Long sectionId);
    
    /**
     * Count the number of active enrollments for a given section.
     * 
     * @param sectionId the section ID
     * @param isActive whether to count only active enrollments
     * @return count of enrollments
     */
    long countBySectionIdAndIsActive(Long sectionId, Boolean isActive);
    
    /**
     * Check if a user has any enrollments (used for user deletion validation).
     * 
     * @param userId the user ID
     * @return true if user has enrollments, false otherwise
     */
    boolean existsByUserId(String userId);
}

