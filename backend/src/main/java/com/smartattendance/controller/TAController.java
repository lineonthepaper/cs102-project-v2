package com.smartattendance.controller;

import com.smartattendance.dto.request.user.AddTARequest;
import com.smartattendance.dto.response.user.TADTO;
import com.smartattendance.dto.request.user.UpdateTAAssignmentsRequest;
import com.smartattendance.service.TAService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teaching-assistants")
@CrossOrigin(origins = "http://localhost:5173")
public class TAController {

    private final TAService taService;

    public TAController(TAService taService) {
        this.taService = taService;
    }

    @GetMapping
    public ResponseEntity<List<TADTO>> getAllTAs() {
        List<TADTO> tas = taService.getAllTAs();
        return ResponseEntity.ok(tas);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addTA(@RequestBody AddTARequest request) {
        try {
            TADTO newTA = taService.addTA(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Teaching Assistant created successfully.", "id", newTA.getId()));
        } catch (RuntimeException e) {
            System.err.println("Error adding TA: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeTA(@PathVariable String id) {
        try {
            taService.removeTA(id);
            return ResponseEntity.ok(Map.of("message", "Teaching Assistant removed successfully."));
        } catch (RuntimeException e) {
            System.err.println("Error removing TA: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/assignments")
    public ResponseEntity<Map<String, String>> updateTAAssignments(
            @PathVariable String id,
            @RequestBody UpdateTAAssignmentsRequest request) {
        try {
            taService.updateTAAssignments(id, request);
            return ResponseEntity.ok(Map.of("message", "TA assignments updated successfully."));
        } catch (RuntimeException e) {
            System.err.println("Error updating TA assignments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}

