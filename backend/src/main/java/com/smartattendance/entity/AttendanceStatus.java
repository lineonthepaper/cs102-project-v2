package com.smartattendance.entity;

import lombok.Getter;

@Getter
public enum AttendanceStatus {
    PRESENT("PRESENT", "Present"),
    LATE("LATE", "Late"),
    ABSENT("ABSENT", "Absent");
    
    private final String code;
    private final String displayName;
    
    AttendanceStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    /**
     * Convert string code to AttendanceStatus enum (case-insensitive).
     * @param code the status code (e.g., "PRESENT", "present", "Present")
     * @return the corresponding AttendanceStatus
     * @throws IllegalArgumentException if code is invalid
     */
    public static AttendanceStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AttendanceStatus status : AttendanceStatus.values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid attendance status: " + code + ". Must be PRESENT, LATE, or ABSENT");
    }
    
    /**
     * Check if this status represents being present (includes PRESENT and LATE).
     * A student who is late is still considered present (attended the class).
     * 
     * @return true if status is PRESENT or LATE
     */
    public boolean isPresent() {
        return this == PRESENT || this == LATE;
    }
    
    /**
     * Check if this status represents being late.
     * 
     * @return true if status is LATE
     */
    public boolean isLate() {
        return this == LATE;
    }
    
    /**
     * Check if this status represents being absent.
     * 
     * @return true if status is ABSENT
     */
    public boolean isAbsent() {
        return this == ABSENT;
    }
    
    /**
     * Check if this status represents being on time (present but not late).
     * 
     * @return true if status is PRESENT
     */
    public boolean isOnTime() {
        return this == PRESENT;
    }
    
    @Override
    public String toString() {
        return code;
    }
}

