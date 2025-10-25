package com.smartattendance.controller;

import com.smartattendance.dto.request.attendance.CreateAttendanceSessionRequest;
import com.smartattendance.dto.request.attendance.MarkAttendanceRequest;
import com.smartattendance.dto.response.attendance.AttendanceRecordResponseDTO;
import com.smartattendance.dto.response.attendance.AttendanceSessionResponseDTO;
import com.smartattendance.service.AttendanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:5173")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AttendanceSessionResponseDTO>> getAllSessions() {
        List<AttendanceSessionResponseDTO> sessions = attendanceService.getAllSessions();
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody CreateAttendanceSessionRequest request) {
        AttendanceSessionResponseDTO session = attendanceService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<?> updateSession(
            @PathVariable Long id,
            @RequestBody CreateAttendanceSessionRequest request) {
        AttendanceSessionResponseDTO session = attendanceService.updateSession(id, request);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{id}/records")
    public ResponseEntity<List<AttendanceRecordResponseDTO>> getSessionRecords(
            @PathVariable Long id) {
        List<AttendanceRecordResponseDTO> records = attendanceService.getSessionRecords(id);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/records")
    public ResponseEntity<?> markAttendance(@RequestBody MarkAttendanceRequest request) {
        AttendanceRecordResponseDTO record = attendanceService.markAttendance(request);
        return ResponseEntity.ok(record);
    }
}

