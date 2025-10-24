package com.smartattendance.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private String id;
    private String displayId;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    
    // Attendance statistics
    private Integer totalSessions;
    private Integer presentSessions;
    private Integer lateSessions;
    private Integer attendanceRate;
    private Integer punctualityRate;
    
    // Relationships
    private List<EnrollmentDTO> enrollments;
    private List<AttendanceRecordDTO> attendanceRecords;
}

