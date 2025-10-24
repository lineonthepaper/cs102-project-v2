package com.smartattendance.repository;

import com.smartattendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    
    @Query("SELECT ar FROM AttendanceRecord ar " +
           "JOIN FETCH ar.attendanceSession ats " +
           "JOIN FETCH ats.section s " +
           "JOIN FETCH s.course c " +
           "WHERE ar.userId = :userId")
    List<AttendanceRecord> findByUserIdWithDetails(@Param("userId") String userId);
    
    @Query("SELECT ar FROM AttendanceRecord ar " +
           "WHERE ar.userId IN :userIds")
    List<AttendanceRecord> findByUserIdIn(@Param("userIds") List<String> userIds);
    
    List<AttendanceRecord> findBySessionId(Long sessionId);
    Optional<AttendanceRecord> findBySessionIdAndUserId(Long sessionId, String userId);
}

