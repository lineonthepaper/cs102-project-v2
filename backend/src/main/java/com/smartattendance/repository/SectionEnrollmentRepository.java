package com.smartattendance.repository;

import com.smartattendance.entity.SectionEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SectionEnrollmentRepository extends JpaRepository<SectionEnrollment, Long> {
    List<SectionEnrollment> findByUserIdAndIsActive(String userId, Boolean isActive);
    Optional<SectionEnrollment> findByUserIdAndSectionId(String userId, Long sectionId);
}

