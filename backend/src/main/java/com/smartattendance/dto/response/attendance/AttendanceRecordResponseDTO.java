package com.smartattendance.dto.response.attendance;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordResponseDTO {
    private Long id;
    private String userId;
    private Long sessionId;
    private String status;
    private String checkinTime;
    private String checkoutTime;
    private String notes;
}