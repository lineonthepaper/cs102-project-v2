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
        try {
            AttendanceSessionResponseDTO session = attendanceService.createSession(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<?> updateSession(
            @PathVariable Long id,
            @RequestBody CreateAttendanceSessionRequest request) {
        try {
            AttendanceSessionResponseDTO session = attendanceService.updateSession(id, request);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}/records")
    public ResponseEntity<List<AttendanceRecordResponseDTO>> getSessionRecords(
            @PathVariable Long sessionId) {
        List<AttendanceRecordResponseDTO> records = attendanceService.getSessionRecords(sessionId);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/records")
    public ResponseEntity<?> markAttendance(@RequestBody MarkAttendanceRequest request) {
        try {
            AttendanceRecordResponseDTO record = attendanceService.markAttendance(request);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

