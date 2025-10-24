package com.smartattendance.dto.request.attendance;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttendanceSessionRequest {
    private Long sectionId;
    private String sessionDate;
    private String scheduledStartTime;
    private String scheduledEndTime;
    private String status;
    private String notes;
}

