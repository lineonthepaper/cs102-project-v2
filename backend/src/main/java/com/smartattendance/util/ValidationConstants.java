package com.smartattendance.util;

/**
 * Constants used for input validation across the application.
 * Centralizes validation rules to maintain consistency and ease of maintenance.
 */
public class ValidationConstants {
    
    // User/Person Name Validation
    public static final int NAME_MIN_LENGTH = 1;
    public static final int NAME_MAX_LENGTH = 50;
    public static final String NAME_MIN_MESSAGE = "Name must be at least " + NAME_MIN_LENGTH + " character";
    public static final String NAME_MAX_MESSAGE = "Name must not exceed " + NAME_MAX_LENGTH + " characters";
    
    // Email Validation
    public static final int EMAIL_MAX_LENGTH = 255;
    public static final String EMAIL_REQUIRED_MESSAGE = "Email is required";
    public static final String EMAIL_INVALID_MESSAGE = "Email must be a valid email address";
    
    // Password Validation
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 100;
    public static final String PASSWORD_MIN_MESSAGE = "Password must be at least " + PASSWORD_MIN_LENGTH + " characters";
    public static final String PASSWORD_MAX_MESSAGE = "Password must not exceed " + PASSWORD_MAX_LENGTH + " characters";
    
    // Course Code Validation
    public static final int COURSE_CODE_MIN_LENGTH = 2;
    public static final int COURSE_CODE_MAX_LENGTH = 20;
    
    // Section Code Validation
    public static final int SECTION_CODE_MIN_LENGTH = 2;
    public static final int SECTION_CODE_MAX_LENGTH = 50;
    
    // Description Validation
    public static final int DESCRIPTION_MAX_LENGTH = 500;
    
    // Location Validation
    public static final int LOCATION_MAX_LENGTH = 100;
    
    // Notes Validation
    public static final int NOTES_MAX_LENGTH = 1000;
    
    private ValidationConstants() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

