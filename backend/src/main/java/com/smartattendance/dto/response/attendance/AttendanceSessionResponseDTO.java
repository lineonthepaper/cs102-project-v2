package com.smartattendance.dto.response.attendance;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSessionResponseDTO {
    private Long id;
    private Long sectionId;
    private String sessionDate;
    private String scheduledStartTime;
    private String scheduledEndTime;
    private String status;
    private String notes;
    private SectionInfoDTO section;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionInfoDTO {
        private String sectionCode;
        private Integer year;
        private Integer semester;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private String location;
        private CourseInfoDTO course;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInfoDTO {
        private String code;
        private String title;
    }
}