package com.smartattendance.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionDTO {
    private Long id;
    private String sectionCode;
    private Long courseId;
    private Integer year;
    private Integer semester;
    private String meetingDay; // Day name (e.g., "Monday")
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private CourseDTO course;
}

