package com.smartattendance.controller;

import com.smartattendance.dto.request.user.CreateStudentRequest;
import com.smartattendance.dto.response.user.StudentDTO;
import com.smartattendance.dto.request.user.UpdateEnrollmentRequest;
import com.smartattendance.service.StudentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "http://localhost:5173")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false, defaultValue = "false") boolean summary) {
        logger.info("Fetching students - summary: {}, courseId: {}, sectionId: {}", 
                    summary, courseId, sectionId);
        
        List<StudentDTO> students;
        if (summary) {
            // Lightweight endpoint for list views
            students = studentService.getStudentsSummary(courseId, sectionId);
        } else {
            // Full data with attendance records (for compatibility)
            students = studentService.getAllStudents();
        }
        
        logger.info("Retrieved {} students", students.size());
        return ResponseEntity.ok(students);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        logger.info("Creating student with email: {}", request.getEmail());
        StudentDTO student = studentService.createStudent(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Student added successfully!");
        response.put("student", student);
        
        logger.info("Student created successfully with ID: {}", student.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{studentId}/enrollments")
    public ResponseEntity<Map<String, Object>> updateEnrollments(
            @PathVariable String studentId,
            @Valid @RequestBody UpdateEnrollmentRequest request) {
        logger.info("Updating enrollments for student: {}", studentId);
        studentService.updateEnrollments(studentId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Enrollments updated successfully");
        
        logger.info("Enrollments updated successfully for student: {}", studentId);
        return ResponseEntity.ok(response);
    }
}

