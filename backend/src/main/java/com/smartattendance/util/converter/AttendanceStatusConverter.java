package com.smartattendance.util.converter;

import com.smartattendance.entity.AttendanceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for AttendanceStatus enum.
 * 
 * Stores status as VARCHAR in database ("PRESENT", "LATE", "ABSENT")
 * but uses type-safe enum in Java code.
 * 
 * Database: VARCHAR ("PRESENT", "LATE", "ABSENT")
 * Java: AttendanceStatus enum (PRESENT, LATE, ABSENT)
 * 
 * Note: autoApply = false means this must be explicitly specified with @Convert annotation
 * on fields that use it. This gives more control over which String fields are attendance statuses.
 */
@Converter(autoApply = false)
public class AttendanceStatusConverter implements AttributeConverter<AttendanceStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(AttendanceStatus status) {
        if (status == null) {
            return null;
        }
        return status.getCode();
    }
    
    @Override
    public AttendanceStatus convertToEntityAttribute(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return AttendanceStatus.fromCode(code);
    }
}

