package com.smartattendance.controller;

import com.smartattendance.dto.response.SectionDTO;
import com.smartattendance.service.SectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sections")
@CrossOrigin(origins = "http://localhost:5173")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    public ResponseEntity<List<SectionDTO>> getAllSections() {
        List<SectionDTO> sections = sectionService.getAllSections();
        return ResponseEntity.ok(sections);
    }

    @PostMapping
    public ResponseEntity<SectionDTO> createSection(@RequestBody SectionDTO sectionDTO) {
        try {
            SectionDTO createdSection = sectionService.createSection(sectionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSection);
        } catch (RuntimeException e) {
            System.err.println("Error creating section: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SectionDTO> updateSection(@PathVariable Long id, @RequestBody SectionDTO sectionDTO) {
        try {
            SectionDTO updatedSection = sectionService.updateSection(id, sectionDTO);
            return ResponseEntity.ok(updatedSection);
        } catch (RuntimeException e) {
            System.err.println("Error updating section: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSection(@PathVariable Long id) {
        try {
            sectionService.deleteSection(id);
            return ResponseEntity.ok(Map.of("message", "Section deleted successfully"));
        } catch (RuntimeException e) {
            System.err.println("Error deleting section: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}

