package com.smartattendance.repository;

import com.smartattendance.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findBySectionId(Long sectionId);
    
    /**
     * Check if any attendance sessions exist for a given section.
     * Used for dependency validation before section deletion.
     * 
     * @param sectionId the section ID to check
     * @return true if sessions exist, false otherwise
     */
    boolean existsBySectionId(Long sectionId);
    
    /**
     * Count the number of attendance sessions for a given section.
     * 
     * @param sectionId the section ID
     * @return count of sessions
     */
    long countBySectionId(Long sectionId);
}

