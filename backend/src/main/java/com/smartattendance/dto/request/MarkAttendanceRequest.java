package com.smartattendance.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkAttendanceRequest {
    private Long sessionId;
    private String userId;
    private String status;
    private String checkinTime;
    private String notes;
}

