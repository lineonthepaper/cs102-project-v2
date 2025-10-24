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
}

