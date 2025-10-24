package com.smartattendance.repository;

import com.smartattendance.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    /**
     * Check if any sections exist for a given course.
     * Used for dependency validation before course deletion.
     * 
     * @param courseId the course ID to check
     * @return true if sections exist, false otherwise
     */
    boolean existsByCourseId(Long courseId);
    
    /**
     * Count the number of sections for a given course.
     * Useful for providing detailed error messages.
     * 
     * @param courseId the course ID
     * @return count of sections
     */
    long countByCourseId(Long courseId);
    
    /**
     * Find all sections for a given course.
     * 
     * @param courseId the course ID
     * @return list of sections
     */
    List<Section> findByCourseId(Long courseId);
}

