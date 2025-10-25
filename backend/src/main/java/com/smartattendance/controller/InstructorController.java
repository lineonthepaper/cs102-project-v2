package com.smartattendance.controller;

import com.smartattendance.dto.request.user.AddInstructorRequest;
import com.smartattendance.dto.response.user.InstructorDTO;
import com.smartattendance.dto.request.user.UpdateInstructorAssignmentsRequest;
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
        InstructorDTO newInstructor = instructorService.addInstructor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Instructor account created successfully.", "id", newInstructor.getId()));
        
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeInstructor(@PathVariable String id) {
        instructorService.removeInstructor(id);
        return ResponseEntity.ok(Map.of("message", "Instructor removed successfully."));
    }

    @PutMapping("/{id}/assignments")
    public ResponseEntity<Map<String, String>> updateInstructorAssignments(
            @PathVariable String id,
            @RequestBody UpdateInstructorAssignmentsRequest request) {
        instructorService.updateInstructorAssignments(id, request);
        return ResponseEntity.ok(Map.of("message", "Instructor assignments updated successfully."));
    }
}

