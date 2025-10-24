package com.smartattendance.entity;

/**
 * Enumeration representing academic semesters.
 * Replaces primitive Integer values (1, 2, 3) with type-safe enum.
 * 
 * This addresses primitive obsession anti-pattern and provides:
 * - Type safety (can't accidentally use invalid semester values)
 * - Self-documenting code (Semester.FALL vs integer 1)
 * - Encapsulated behavior (display names, validation)
 */
public enum Semester {
    FALL(1, "Fall"),
    SPRING(2, "Spring"),
    SUMMER(3, "Summer");
    
    private final int value;
    private final String displayName;
    
    Semester(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Convert integer value to Semester enum.
     * @param value the integer value (1, 2, or 3)
     * @return the corresponding Semester
     * @throws IllegalArgumentException if value is invalid
     */
    public static Semester fromValue(int value) {
        for (Semester semester : Semester.values()) {
            if (semester.value == value) {
                return semester;
            }
        }
        throw new IllegalArgumentException("Invalid semester value: " + value + ". Must be 1 (Fall), 2 (Spring), or 3 (Summer)");
    }
    
    /**
     * Convert display name to Semester enum (case-insensitive).
     * @param displayName the display name (e.g., "Fall", "fall", "FALL")
     * @return the corresponding Semester
     * @throws IllegalArgumentException if name is invalid
     */
    public static Semester fromDisplayName(String displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("Semester display name cannot be null");
        }
        for (Semester semester : Semester.values()) {
            if (semester.displayName.equalsIgnoreCase(displayName)) {
                return semester;
            }
        }
        throw new IllegalArgumentException("Invalid semester name: " + displayName);
    }
    
    /**
     * Check if this is a fall semester.
     */
    public boolean isFall() {
        return this == FALL;
    }
    
    /**
     * Check if this is a spring semester.
     */
    public boolean isSpring() {
        return this == SPRING;
    }
    
    /**
     * Check if this is a summer semester.
     */
    public boolean isSummer() {
        return this == SUMMER;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}

