package com.smartattendance.controller;

import com.smartattendance.dto.request.user.CreateStudentRequest;
import com.smartattendance.dto.response.user.StudentDTO;
import com.smartattendance.dto.request.user.UpdateEnrollmentRequest;
import com.smartattendance.dto.request.user.UpdateStudentRequest;
import com.smartattendance.service.StudentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importStudents(
            @RequestParam("file") MultipartFile zipFile) {

        logger.info("Importing students from ZIP file: {}", zipFile.getOriginalFilename());

        try {
            if (!zipFile.getOriginalFilename().toLowerCase().endsWith(".zip")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File must be a ZIP file"));
            }

            Map<String, Object> result = studentService.importStudentsFromZip(zipFile);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to import from ZIP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to process ZIP file: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable String id) {
        logger.info("Fetching student by ID: {}", id);
        StudentDTO student = studentService.getStudentById(id);
        logger.info("Retrieved student: {} {}", student.getFirstName(), student.getLastName());
        return ResponseEntity.ok(student);
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

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateStudent(
            @PathVariable String id,
            @Valid @RequestBody UpdateStudentRequest request) {
        logger.info("Updating student: {}", id);
        StudentDTO student = studentService.updateStudent(id, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Student updated successfully!");
        response.put("student", student);

        logger.info("Student updated successfully: {}", id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/enrollments")
    public ResponseEntity<Map<String, Object>> updateEnrollments(
            @PathVariable String id,
            @Valid @RequestBody UpdateEnrollmentRequest request) {
        logger.info("Updating enrollments for student: {}", id);
        studentService.updateEnrollments(id, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Enrollments updated successfully");

        logger.info("Enrollments updated successfully for student: {}", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteStudent(@PathVariable String id) {
        logger.info("Deleting student: {}", id);
        studentService.deleteStudent(id);
        logger.info("Student deleted successfully: {}", id);
        return ResponseEntity.ok(Map.of("message", "Student deleted successfully"));
    }

}
