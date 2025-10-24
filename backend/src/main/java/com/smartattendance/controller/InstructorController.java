package com.smartattendance.controller;

import com.smartattendance.dto.request.AddInstructorRequest;
import com.smartattendance.dto.response.InstructorDTO;
import com.smartattendance.dto.request.UpdateInstructorAssignmentsRequest;
import com.smartattendance.service.InstructorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instructors")
@CrossOrigin(origins = "http://localhost:5173")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @GetMapping
    public ResponseEntity<List<InstructorDTO>> getAllInstructors() {
        List<InstructorDTO> instructors = instructorService.getAllInstructors();
        return ResponseEntity.ok(instructors);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addInstructor(@RequestBody AddInstructorRequest request) {
        try {
            InstructorDTO newInstructor = instructorService.addInstructor(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Instructor account created successfully.", "id", newInstructor.getId()));
        } catch (RuntimeException e) {
            System.err.println("Error adding instructor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> removeInstructor(@PathVariable String userId) {
        try {
            instructorService.removeInstructor(userId);
            return ResponseEntity.ok(Map.of("message", "Instructor removed successfully."));
        } catch (RuntimeException e) {
            System.err.println("Error removing instructor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/assignments")
    public ResponseEntity<Map<String, String>> updateInstructorAssignments(
            @PathVariable String userId,
            @RequestBody UpdateInstructorAssignmentsRequest request) {
        try {
            instructorService.updateInstructorAssignments(userId, request);
            return ResponseEntity.ok(Map.of("message", "Instructor assignments updated successfully."));
        } catch (RuntimeException e) {
            System.err.println("Error updating instructor assignments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}

