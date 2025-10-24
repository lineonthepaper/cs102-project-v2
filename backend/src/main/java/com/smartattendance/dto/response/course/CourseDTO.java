package com.smartattendance.dto.response.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for Course.
 * 
 * FIXED: Added validation annotations (Phase 3)
 * - Ensures data integrity at API boundary
 * - Provides clear error messages
 * - Works with @Valid annotation in controllers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    
    @NotBlank(message = "Course code is required")
    @Pattern(
        regexp = "^[A-Z]{2,4}\\d{3}$", 
        message = "Course code must be 2-4 uppercase letters followed by 3 digits (e.g., CS102, MATH101)"
    )
    private String code;
    
    @NotBlank(message = "Course title is required")
    @Size(min = 3, max = 200, message = "Course title must be between 3 and 200 characters")
    private String title;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}