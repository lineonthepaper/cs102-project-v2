package com.smartattendance.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordDTO {
    private Long id;
    private String userId;
    private Long sessionId;
    private String status; // PRESENT, LATE, ABSENT
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private AttendanceSessionDTO attendanceSession;
}