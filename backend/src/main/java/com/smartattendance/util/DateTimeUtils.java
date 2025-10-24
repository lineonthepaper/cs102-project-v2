package com.smartattendance.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for date and time related operations.
 * Provides conversion between day numbers and day names used throughout the application.
 */
public class DateTimeUtils {
    
    private static final Map<Integer, String> DAY_NUMBER_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> DAY_NAME_TO_NUMBER = new HashMap<>();
    
    static {
        // Initialize day mappings (1 = Monday, 7 = Sunday)
        DAY_NUMBER_TO_NAME.put(1, "Monday");
        DAY_NUMBER_TO_NAME.put(2, "Tuesday");
        DAY_NUMBER_TO_NAME.put(3, "Wednesday");
        DAY_NUMBER_TO_NAME.put(4, "Thursday");
        DAY_NUMBER_TO_NAME.put(5, "Friday");
        DAY_NUMBER_TO_NAME.put(6, "Saturday");
        DAY_NUMBER_TO_NAME.put(7, "Sunday");
        
        // Reverse mapping
        DAY_NAME_TO_NUMBER.put("Monday", 1);
        DAY_NAME_TO_NUMBER.put("Tuesday", 2);
        DAY_NAME_TO_NUMBER.put("Wednesday", 3);
        DAY_NAME_TO_NUMBER.put("Thursday", 4);
        DAY_NAME_TO_NUMBER.put("Friday", 5);
        DAY_NAME_TO_NUMBER.put("Saturday", 6);
        DAY_NAME_TO_NUMBER.put("Sunday", 7);
    }
    
    /**
     * Converts a day number to its corresponding day name.
     *
     * @param dayNumber the day number (1-7, where 1 = Monday)
     * @return the day name, or "Unknown" if invalid
     */
    public static String dayNumberToName(Integer dayNumber) {
        if (dayNumber == null) {
            return null;
        }
        return DAY_NUMBER_TO_NAME.getOrDefault(dayNumber, "Unknown");
    }
    
    /**
     * Converts a day name to its corresponding day number.
     *
     * @param dayName the day name (e.g., "Monday")
     * @return the day number (1-7), or null if invalid
     */
    public static Integer dayNameToNumber(String dayName) {
        if (dayName == null) {
            return null;
        }
        return DAY_NAME_TO_NUMBER.get(dayName);
    }
    
    private DateTimeUtils() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

