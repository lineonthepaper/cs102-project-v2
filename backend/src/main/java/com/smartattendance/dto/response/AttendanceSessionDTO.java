package com.smartattendance.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSessionDTO {
    private Long id;
    private Long sectionId;
    private LocalDate sessionDate;
    private LocalTime scheduledStartTime;
    private LocalTime scheduledEndTime;
    private String status;
    private String notes;
    private SectionDTO section;
}