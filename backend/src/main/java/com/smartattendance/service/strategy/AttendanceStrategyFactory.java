package com.smartattendance.service.strategy;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating the appropriate attendance marking strategy.
 * 
 * This class manages all available strategies and returns the correct one
 * based on the requested status.
 * 
 * Benefits:
 * - Centralizes strategy selection
 * - Automatically discovers all strategy implementations via Spring
 * - Makes adding new strategies trivial (just create the class and Spring registers it)
 */
@Component
public class AttendanceStrategyFactory {
    
    private final Map<String, AttendanceMarkingStrategy> strategies;
    
    /**
     * Constructor that auto-wires all AttendanceMarkingStrategy beans.
     * Spring will automatically inject all implementations.
     * 
     * @param strategyList list of all strategy implementations
     */
    public AttendanceStrategyFactory(List<AttendanceMarkingStrategy> strategyList) {
        this.strategies = new HashMap<>();
        for (AttendanceMarkingStrategy strategy : strategyList) {
            strategies.put(strategy.getStatusName().toUpperCase(), strategy);
        }
    }
    
    /**
     * Get the appropriate strategy for the given status.
     * 
     * @param status the attendance status (e.g., "PRESENT", "LATE", "ABSENT")
     * @return the strategy for that status
     * @throws IllegalArgumentException if status is unknown
     */
    public AttendanceMarkingStrategy getStrategy(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Attendance status cannot be null");
        }
        
        AttendanceMarkingStrategy strategy = strategies.get(status.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown attendance status: " + status);
        }
        
        return strategy;
    }
    
    /**
     * Check if a status is supported.
     * 
     * @param status the status to check
     * @return true if supported, false otherwise
     */
    public boolean isSupported(String status) {
        return status != null && strategies.containsKey(status.toUpperCase());
    }
}

