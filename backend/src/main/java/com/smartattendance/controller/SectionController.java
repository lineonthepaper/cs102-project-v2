package com.smartattendance.controller;

import com.smartattendance.dto.response.course.SectionDTO;
import com.smartattendance.dto.response.user.StudentDTO;
import com.smartattendance.service.SectionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sections")
@CrossOrigin(origins = "http://localhost:5173")
public class SectionController {

    private static final Logger logger = LoggerFactory.getLogger(SectionController.class);
    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    public ResponseEntity<List<SectionDTO>> getAllSections() {
        logger.info("Fetching all sections");
        List<SectionDTO> sections = sectionService.getAllSections();
        logger.info("Retrieved {} sections", sections.size());
        return ResponseEntity.ok(sections);
    }

    @PostMapping
    public ResponseEntity<SectionDTO> createSection(@Valid @RequestBody SectionDTO sectionDTO) {
        logger.info("Creating section: {}", sectionDTO.getSectionCode());
        SectionDTO createdSection = sectionService.createSection(sectionDTO);
        logger.info("Section created successfully with ID: {}", createdSection.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSection);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SectionDTO> updateSection(@PathVariable Long id, @Valid @RequestBody SectionDTO sectionDTO) {
        logger.info("Updating section with ID: {}", id);
        SectionDTO updatedSection = sectionService.updateSection(id, sectionDTO);
        logger.info("Section updated successfully: {}", id);
        return ResponseEntity.ok(updatedSection);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSection(@PathVariable Long id) {
        logger.info("Deleting section with ID: {}", id);
        sectionService.deleteSection(id);
        logger.info("Section deleted successfully: {}", id);
        return ResponseEntity.ok(Map.of("message", "Section deleted successfully"));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<List<StudentDTO>> getStudentsBySection(@PathVariable Long id) {
        logger.info("Fetching students for section ID: {}", id);
        List<StudentDTO> students = sectionService.getStudentsBySection(id);
        logger.info("Retrieved {} students for section {}", students.size(), id);
        return ResponseEntity.ok(students);
    }
}

