package com.smartattendance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.StringWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class FullDatabaseExportService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public String exportEntireDatabase() throws IOException {
        StringWriter writer = new StringWriter();
        
        // Export each table
        exportUsers(writer);
        writer.write("\n\n");
        
        exportCourses(writer);
        writer.write("\n\n");
        
        exportSections(writer);
        writer.write("\n\n");
        
        exportSectionEnrollments(writer);
        writer.write("\n\n");
        
        exportSectionAssignments(writer);
        writer.write("\n\n");
        
        exportTAAssignments(writer);
        writer.write("\n\n");
        
        exportAttendanceSessions(writer);
        writer.write("\n\n");
        
        exportAttendanceRecords(writer);
        
        return writer.toString();
    }
    
    private void exportUsers(StringWriter writer) throws IOException {
        writer.write("=== USERS TABLE ===\n");
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM users");
        
        if (!data.isEmpty()) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "email", "first_name", "last_name", "auth_id", 
                          "is_student", "is_instructor", "is_ta", "enabled", 
                          "created_at", "updated_at", "face_images", "face_profiles")
                .build();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : data) {
                    csvPrinter.printRecord(
                        row.get("id"), row.get("email"), row.get("first_name"),
                        row.get("last_name"), row.get("auth_id"), row.get("is_student"),
                        row.get("is_instructor"), row.get("is_ta"), row.get("enabled"),
                        row.get("created_at"), row.get("updated_at"), 
                        row.get("face_images"), row.get("face_profiles")
                    );
                }
            }
        }
    }
    
    private void exportCourses(StringWriter writer) throws IOException {
        writer.write("=== COURSES TABLE ===\n");
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM courses");
        
        if (!data.isEmpty()) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "code", "title", "description", "is_active", 
                          "created_at", "updated_at")
                .build();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : data) {
                    csvPrinter.printRecord(
                        row.get("id"), row.get("code"), row.get("title"),
                        row.get("description"), row.get("is_active"),
                        row.get("created_at"), row.get("updated_at")
                    );
                }
            }
        }
    }
    
    private void exportSections(StringWriter writer) throws IOException {
        writer.write("=== SECTIONS TABLE ===\n");
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM sections");
        
        if (!data.isEmpty()) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "course_id", "section_code", "meeting_day", "start_time",
                          "end_time", "location", "max_capacity", "day_of_week", "schedule",
                          "created_at", "updated_at", "year", "semester")
                .build();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : data) {
                    csvPrinter.printRecord(
                        row.get("id"), row.get("course_id"), row.get("section_code"),
                        row.get("meeting_day"), row.get("start_time"), row.get("end_time"),
                        row.get("location"), row.get("max_capacity"), row.get("day_of_week"),
                        row.get("schedule"), row.get("created_at"), row.get("updated_at"),
                        row.get("year"), row.get("semester")
                    );
                }
            }
        }
    }
    
    private void exportSectionEnrollments(StringWriter writer) throws IOException {
        writer.write("=== SECTION_ENROLLMENTS TABLE ===\n");
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM section_enrollments");
        
        if (!data.isEmpty()) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "section_id", "user_id", "is_active", "enrolled_at", "updated_at")
                .build();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : data) {
                    csvPrinter.printRecord(
                        row.get("id"), row.get("section_id"), row.get("user_id"),
                        row.get("is_active"), row.get("enrolled_at"), row.get("updated_at")
                    );
                }
            }
        }
    }
    
    private void exportSectionAssignments(StringWriter writer) throws IOException {
        writer.write("=== SECTION_ASSIGNMENTS TABLE ===\n");
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM section_assignments");
        
        if (!data.isEmpty()) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "section_id", "user_id", "role", "is_active", "assigned_at", "updated_at")
                .build();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : data) {
                    csvPrinter.printRecord(
                        row.get("id"), row.get("section_id"), row.get("user_id"),
                        row.get("role"), row.get("is_active"), row.get("assigned_at"),
                        row.get("updated_at")
                    );
                }
            }
        }
    }
    
    private void exportTAAssignments(StringWriter writer) throws IOException {
        writer.write("=== TA_ASSIGNMENTS TABLE ===\n");
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM ta_assignments");
        
        if (!data.isEmpty()) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "user_id", "course_id", "section_id", "assigned_at", 
                          "is_active", "updated_at")
                .build();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : data) {
                    csvPrinter.printRecord(
                        row.get("id"), row.get("user_id"), row.get("course_id"),
                        row.get("section_id"), row.get("assigned_at"), row.get("is_active"),
                        row.get("updated_at")
                    );
                }
            }
        }
    }
    
    private void exportAttendanceSessions(StringWriter writer) throws IOException {
        writer.write("=== ATTENDANCE_SESSIONS TABLE ===\n");
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM attendance_sessions");
        
        if (!data.isEmpty()) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "section_id", "session_date", "scheduled_start_time",
                          "scheduled_end_time", "status", "notes", "created_at", "updated_at")
                .build();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : data) {
                    csvPrinter.printRecord(
                        row.get("id"), row.get("section_id"), row.get("session_date"),
                        row.get("scheduled_start_time"), row.get("scheduled_end_time"),
                        row.get("status"), row.get("notes"), row.get("created_at"),
                        row.get("updated_at")
                    );
                }
            }
        }
    }
    
    private void exportAttendanceRecords(StringWriter writer) throws IOException {
        writer.write("=== ATTENDANCE_RECORDS TABLE ===\n");
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM attendance_records");
        
        if (!data.isEmpty()) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "session_id", "user_id", "status", "checkin_time",
                          "checkout_time", "notes", "created_at", "updated_at")
                .build();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : data) {
                    csvPrinter.printRecord(
                        row.get("id"), row.get("session_id"), row.get("user_id"),
                        row.get("status"), row.get("checkin_time"), row.get("checkout_time"),
                        row.get("notes"), row.get("created_at"), row.get("updated_at")
                    );
                }
            }
        }
    }
}