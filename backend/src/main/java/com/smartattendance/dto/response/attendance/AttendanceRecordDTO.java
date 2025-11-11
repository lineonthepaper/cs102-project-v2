package com.smartattendance.dto.response.attendance;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordDTO {
    private Long id;
    private String userId;
    private Long sessionId;
    private String status; // PRESENT, LATE, ABSENT
    private OffsetDateTime checkinTime;
    private OffsetDateTime checkoutTime;
    private AttendanceSessionDTO attendanceSession;
}