package com.smartattendance.service.strategy;

import com.smartattendance.entity.AttendanceRecord;
import com.smartattendance.util.constants.AttendanceConstants;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

/**
 * Strategy for marking students as absent.
 * Handles the "ABSENT" attendance status.
 * 
 * Note: For absent, checkinTime is ignored as the student didn't check in.
 */
@Component
public class AbsentAttendanceStrategy implements AttendanceMarkingStrategy {
    
    @Override
    public void mark(AttendanceRecord record, OffsetDateTime checkinTime) {
        record.markAbsent();
        // Note: markAbsent() clears checkinTime as students who are absent didn't check in
    }
    
    @Override
    public String getStatusName() {
        return AttendanceConstants.STATUS_ABSENT;
    }
}

