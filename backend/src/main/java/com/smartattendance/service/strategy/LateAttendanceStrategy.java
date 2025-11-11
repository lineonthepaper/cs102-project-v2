package com.smartattendance.service.strategy;

import com.smartattendance.entity.AttendanceRecord;
import com.smartattendance.util.constants.AttendanceConstants;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

/**
 * Strategy for marking students as late.
 * Handles the "LATE" attendance status.
 */
@Component
public class LateAttendanceStrategy implements AttendanceMarkingStrategy {
    
    @Override
    public void mark(AttendanceRecord record, OffsetDateTime checkinTime) {
        record.markLate(checkinTime);
    }
    
    @Override
    public String getStatusName() {
        return AttendanceConstants.STATUS_LATE;
    }
}

