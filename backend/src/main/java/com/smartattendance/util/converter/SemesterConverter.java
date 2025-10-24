package com.smartattendance.util.converter;

import com.smartattendance.entity.Semester;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for Semester enum.
 * 
 * Stores semester as integer in database (1, 2, 3) for backward compatibility
 * but uses type-safe enum in Java code for better type safety and readability.
 * 
 * Database: INTEGER (1, 2, 3)
 * Java: Semester enum (FALL, SPRING, SUMMER)
 * 
 * @Converter(autoApply = true) means this converter is automatically applied
 * to all Semester fields in entities without needing @Convert annotation.
 */
@Converter(autoApply = true)
public class SemesterConverter implements AttributeConverter<Semester, Integer> {
    
    @Override
    public Integer convertToDatabaseColumn(Semester semester) {
        if (semester == null) {
            return null;
        }
        return semester.getValue();
    }
    
    @Override
    public Semester convertToEntityAttribute(Integer value) {
        if (value == null) {
            return null;
        }
        return Semester.fromValue(value);
    }
}

