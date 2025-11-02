package com.smartattendance.controller;

import com.smartattendance.dto.request.attendance.CreateAttendanceSessionRequest;
import com.smartattendance.dto.request.attendance.FaceScanRequest;
import com.smartattendance.dto.request.attendance.MarkAttendanceRequest;
import com.smartattendance.dto.response.attendance.AttendanceRecordResponseDTO;
import com.smartattendance.dto.response.attendance.AttendanceSessionResponseDTO;
import com.smartattendance.dto.response.attendance.FaceScanResponseDTO;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.service.AttendanceService;
import com.smartattendance.service.SessionRecognitionManager;
import jakarta.validation.Valid;

import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:5173")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SessionRecognitionManager recognitionManager;

    public AttendanceController(AttendanceService attendanceService,
                                SessionRecognitionManager recognitionManager) {
        this.attendanceService = attendanceService;
        this.recognitionManager = recognitionManager;
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

    @PostMapping("/sessions/{id}/reopen")
    public ResponseEntity<?> reopenSession(@PathVariable Long id) {
        AttendanceSessionResponseDTO session = attendanceService.reopenSession(id);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/archive")
    public ResponseEntity<?> archiveSession(@PathVariable Long id) {
        AttendanceSessionResponseDTO session = attendanceService.archiveSession(id);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<?> cancelSession(@PathVariable Long id) {
        AttendanceSessionResponseDTO session = attendanceService.cancelSession(id);
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

    @PostMapping("/sessions/{id}/scan")
    public ResponseEntity<FaceScanResponseDTO> scanFace(
            @PathVariable Long id,
            @Valid @RequestBody FaceScanRequest request) {

        byte[] imageBytes = decodeBase64(request.getImageData());
        FaceScanResponseDTO response = recognitionManager.scanFace(id, imageBytes);
        return ResponseEntity.ok(response);
    }

    private byte[] decodeBase64(String imageData) {
        if (imageData == null || imageData.isBlank()) {
            throw new InvalidRequestException("Image data cannot be empty");
        }
        String sanitized = imageData.contains(",") ? imageData.substring(imageData.indexOf(',') + 1) : imageData;
        try {
            return Base64.getDecoder().decode(sanitized);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid base64 image data");
        }
    }
}

