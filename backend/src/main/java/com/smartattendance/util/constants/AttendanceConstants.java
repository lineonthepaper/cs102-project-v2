package com.smartattendance.util.constants;

/**
 * Constants related to attendance management.
 * Centralizes magic strings and numbers for better maintainability.
 */
public final class AttendanceConstants {
    
    // ===== Attendance Status Constants =====
    public static final String STATUS_PRESENT = "PRESENT";
    public static final String STATUS_LATE = "LATE";
    public static final String STATUS_ABSENT = "ABSENT";
    
    // ===== Session Status Constants =====
    public static final String SESSION_STATUS_ACTIVE = "ACTIVE";
    public static final String SESSION_STATUS_ENDED = "ENDED";
    public static final String SESSION_STATUS_CLOSED = "CLOSED";
    public static final String SESSION_STATUS_COMPLETED = "COMPLETED";
    public static final String SESSION_STATUS_SCHEDULED = "SCHEDULED";
    public static final String SESSION_STATUS_ARCHIVED = "ARCHIVED";
    public static final String SESSION_STATUS_CANCELLED = "CANCELLED";
    
    // ===== Timing Constants =====
    /**
     * Number of minutes after scheduled start time that is considered "late".
     * Students checking in after this threshold are marked as late.
     */
    public static final int LATE_THRESHOLD_MINUTES = 5;
    
    /**
     * Number of days that defines a "new" enrollment.
     */
    public static final int NEW_ENROLLMENT_DAYS_THRESHOLD = 7;
    
    // ===== Role Constants =====
    public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
    public static final String ROLE_TA = "TA";
    public static final String ROLE_STUDENT = "STUDENT";
    
    // ===== Semester Constants =====
    public static final int SEMESTER_FALL = 1;
    public static final int SEMESTER_SPRING = 2;
    public static final int SEMESTER_SUMMER = 3;
    
    public static final String SEMESTER_NAME_FALL = "Fall";
    public static final String SEMESTER_NAME_SPRING = "Spring";
    public static final String SEMESTER_NAME_SUMMER = "Summer";
    
    // ===== Validation Constants =====
    /**
     * Regex pattern for valid course codes (2-4 letters + 3 digits).
     * Examples: CS102, MATH101, ENGL202
     */
    public static final String COURSE_CODE_PATTERN = "^[A-Z]{2,4}\\d{3}$";
    
    /**
     * Regex pattern for normalized student IDs.
     * Format: One letter + 7 digits (e.g., S0000001)
     */
    public static final String STUDENT_ID_PATTERN = "^[A-Z]\\d{7}$";
    
    // ===== Description Text Constants =====
    public static final String NO_DESCRIPTION = "No description available";
    public static final String UNKNOWN = "Unknown";
    public static final String UNKNOWN_COURSE = "Unknown Course";
    public static final String UNKNOWN_TERM = "Unknown Term";
    public static final String SCHEDULE_NOT_SET = "Schedule not set";
    
    // ===== Course Level Thresholds =====
    public static final int COURSE_LEVEL_INTERMEDIATE_THRESHOLD = 200;
    public static final int COURSE_LEVEL_ADVANCED_THRESHOLD = 300;
    public static final int COURSE_LEVEL_GRADUATE_THRESHOLD = 400;
    
    public static final String COURSE_LEVEL_INTRODUCTORY = "Introductory";
    public static final String COURSE_LEVEL_INTERMEDIATE = "Intermediate";
    public static final String COURSE_LEVEL_ADVANCED = "Advanced";
    public static final String COURSE_LEVEL_GRADUATE = "Graduate";
    
    // ===== Description Length Constants =====
    public static final int SHORT_DESCRIPTION_LENGTH = 100;
    public static final int SHORT_DESCRIPTION_TRUNCATE_LENGTH = 97;
    public static final String TRUNCATION_SUFFIX = "...";
    
    // Private constructor to prevent instantiation
    private AttendanceConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

