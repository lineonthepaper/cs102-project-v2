package com.smartattendance.controller;

import com.smartattendance.dto.response.course.CourseDTO;
import com.smartattendance.service.CourseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Course management.
 * 
 * FIXED: Removed try-catch blocks (SRP violation)
 * - Controllers should only handle HTTP concerns
 * - Error handling delegated to GlobalExceptionHandler
 * - Added proper logging with SLF4J
 * - Added @Valid annotations for input validation
 */
@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:5173")
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        logger.info("Fetching all courses");
        List<CourseDTO> courses = courseService.getAllCourses();
        logger.info("Retrieved {} courses", courses.size());
        return ResponseEntity.ok(courses);
    }

    @PostMapping
    public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody CourseDTO courseDTO) {
        logger.info("Creating course with code: {}", courseDTO.getCode());
        CourseDTO createdCourse = courseService.createCourse(courseDTO);
        logger.info("Course created successfully with ID: {}", createdCourse.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
        // Let GlobalExceptionHandler handle exceptions! ✓
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseDTO courseDTO) {
        logger.info("Updating course with ID: {}", id);
        CourseDTO updatedCourse = courseService.updateCourse(id, courseDTO);
        logger.info("Course updated successfully: {}", id);
        return ResponseEntity.ok(updatedCourse);
        // Let GlobalExceptionHandler handle exceptions! ✓
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCourse(@PathVariable Long id) {
        logger.info("Deleting course with ID: {}", id);
        courseService.deleteCourse(id);
        logger.info("Course deleted successfully: {}", id);
        return ResponseEntity.ok(Map.of("message", "Course deleted successfully"));
        // Let GlobalExceptionHandler handle exceptions! ✓
    }
}

