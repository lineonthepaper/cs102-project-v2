package com.smartattendance.repository;

import com.smartattendance.entity.TAAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TAAssignmentRepository extends JpaRepository<TAAssignment, Long> {
    List<TAAssignment> findByUserId(String userId);
    void deleteByUserId(String userId);
}

