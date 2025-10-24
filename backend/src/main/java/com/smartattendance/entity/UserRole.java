package com.smartattendance.entity;

/**
 * Enumeration representing the different user roles in the system.
 * Each user can have one or more roles: STUDENT, TA (Teaching Assistant), or INSTRUCTOR.
 */
public enum UserRole {
    /**
     * Regular student role - can view attendance and enrolled courses
     */
    STUDENT,
    
    /**
     * Teaching Assistant role - can manage attendance for assigned sections
     */
    TA,
    
    /**
     * Instructor role - can manage courses, sections, and all attendance
     */
    INSTRUCTOR
}

