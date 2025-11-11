package com.smartattendance.service.strategy;

import com.smartattendance.entity.AttendanceRecord;
import com.smartattendance.util.constants.AttendanceConstants;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

/**
 * Strategy for marking students as present.
 * Handles the "PRESENT" attendance status.
 */
@Component
public class PresentAttendanceStrategy implements AttendanceMarkingStrategy {
    
    @Override
    public void mark(AttendanceRecord record, OffsetDateTime checkinTime) {
        record.markPresent(checkinTime);
    }
    
    @Override
    public String getStatusName() {
        return AttendanceConstants.STATUS_PRESENT;
    }
}

