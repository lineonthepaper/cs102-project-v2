package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.smartattendance.util.constants.AttendanceConstants;

@Getter 
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    // ===== VALIDATED SETTERS (ISP Fix) =====
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        if (!code.matches(AttendanceConstants.COURSE_CODE_PATTERN)) {
            throw new IllegalArgumentException(
                "Invalid course code format. Expected: 2-4 uppercase letters followed by 3 digits (e.g., CS102)"
            );
        }
        this.code = code.toUpperCase(); // Normalize to uppercase
    }
    
    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course title cannot be null or empty");
        }
        if (title.length() < 3) {
            throw new IllegalArgumentException("Course title must be at least 3 characters");
        }
        this.title = title.trim();
    }
    
    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====
    
    /**
     * Validates if the course code follows the standard format.
     * Expected format: 2-4 uppercase letters followed by 3 digits (e.g., CS102, MATH101)
     * 
     * @return true if code is valid, false otherwise
     */
    public boolean isValidCourseCode() {
        if (code == null || code.isBlank()) {
            return false;
        }
        return code.matches(AttendanceConstants.COURSE_CODE_PATTERN);
    }
    
    /**
     * Get the formatted title with course code.
     * Example: "CS102 - Data Structures"
     * 
     * @return formatted course title
     */
    public String getFormattedTitle() {
        if (code == null || title == null) {
            return title != null ? title : AttendanceConstants.UNKNOWN_COURSE;
        }
        return code + " - " + title;
    }
    
    /**
     * Check if the course has a description.
     * 
     * @return true if description exists and is not blank
     */
    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }
    
    /**
     * Get the course department code (letters part of course code).
     * Example: "CS102" -> "CS"
     * 
     * @return department code, or empty string if invalid
     */
    public String getDepartmentCode() {
        if (code == null || code.isBlank()) {
            return "";
        }
        return code.replaceAll("\\d", "");
    }
    
    /**
     * Get the course number (numeric part of course code).
     * Example: "CS102" -> "102"
     * 
     * @return course number, or empty string if invalid
     */
    public String getCourseNumber() {
        if (code == null || code.isBlank()) {
            return "";
        }
        return code.replaceAll("[A-Z]", "");
    }
    
    /**
     * Determine course level based on course number.
     * 100-199: Introductory
     * 200-299: Intermediate
     * 300-399: Advanced
     * 400+: Graduate
     * 
     * @return course level description
     */
    public String getCourseLevel() {
        String number = getCourseNumber();
        if (number.isEmpty()) {
            return AttendanceConstants.UNKNOWN;
        }
        
        try {
            int num = Integer.parseInt(number);
            if (num < AttendanceConstants.COURSE_LEVEL_INTERMEDIATE_THRESHOLD) 
                return AttendanceConstants.COURSE_LEVEL_INTRODUCTORY;
            if (num < AttendanceConstants.COURSE_LEVEL_ADVANCED_THRESHOLD) 
                return AttendanceConstants.COURSE_LEVEL_INTERMEDIATE;
            if (num < AttendanceConstants.COURSE_LEVEL_GRADUATE_THRESHOLD) 
                return AttendanceConstants.COURSE_LEVEL_ADVANCED;
            return AttendanceConstants.COURSE_LEVEL_GRADUATE;
        } catch (NumberFormatException e) {
            return AttendanceConstants.UNKNOWN;
        }
    }
    
    /**
     * Get a short description (first 100 characters).
     * 
     * @return truncated description or full description if shorter
     */
    public String getShortDescription() {
        if (!hasDescription()) {
            return AttendanceConstants.NO_DESCRIPTION;
        }
        return description.length() > AttendanceConstants.SHORT_DESCRIPTION_LENGTH
            ? description.substring(0, AttendanceConstants.SHORT_DESCRIPTION_TRUNCATE_LENGTH) 
                + AttendanceConstants.TRUNCATION_SUFFIX
            : description;
    }
}

