package com.smartattendance.dto.response.course;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;

/**
 * Data Transfer Object for Section.
 * 
 * FIXED: Added validation annotations (Phase 3)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionDTO {
    private Long id;
    
    @NotBlank(message = "Section code is required")
    @Size(min = 2, max = 20, message = "Section code must be between 2 and 20 characters")
    private String sectionCode;
    
    @NotNull(message = "Course ID is required")
    @Positive(message = "Course ID must be positive")
    private Long courseId;
    
    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    private Integer year;
    
    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be 1 (Fall), 2 (Spring), or 3 (Summer)")
    @Max(value = 3, message = "Semester must be 1 (Fall), 2 (Spring), or 3 (Summer)")
    private Integer semester;
    
    private String meetingDay; // Day name (e.g., "Monday")
    private LocalTime startTime;
    private LocalTime endTime;
    
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;
    
    private CourseDTO course;
}