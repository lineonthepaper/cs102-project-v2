package com.smartattendance.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartattendance.service.ImportService;

@RestController
@RequestMapping("/api/students/import")
@CrossOrigin(origins = "http://localhost:5173")
public class ImportController {
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);
    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> importStudents(
            @RequestParam("file") MultipartFile zipFile) {

        logger.info("Importing students from ZIP file: {}", zipFile.getOriginalFilename());

        try {
            if (!zipFile.getOriginalFilename().toLowerCase().endsWith(".zip")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File must be a ZIP file"));
            }

            Map<String, Object> result = importService.importStudentsFromZip(zipFile);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to import from ZIP", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to process ZIP file: " + e.getMessage()));
        }
    }
}
