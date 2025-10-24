package com.smartattendance.service.strategy;

import com.smartattendance.entity.AttendanceRecord;
import java.time.LocalDateTime;

/**
 * Strategy interface for marking attendance.
 * 
 * This follows the Strategy Pattern and Open/Closed Principle:
 * - Open for extension: New attendance types can be added by creating new implementations
 * - Closed for modification: Existing code doesn't need to change when new types are added
 * 
 * Benefits:
 * - Eliminates switch statements in services
 * - Makes adding new attendance statuses easy
 * - Each strategy encapsulates its own marking logic
 */
public interface AttendanceMarkingStrategy {
    
    /**
     * Mark the attendance record with the appropriate status and time.
     * 
     * @param record the attendance record to mark
     * @param checkinTime the time of check-in
     */
    void mark(AttendanceRecord record, LocalDateTime checkinTime);
    
    /**
     * Get the status name this strategy handles.
     * 
     * @return the status name (e.g., "PRESENT", "LATE", "ABSENT")
     */
    String getStatusName();
}

