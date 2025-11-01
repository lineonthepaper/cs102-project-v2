package com.smartattendance.controller;

import com.smartattendance.service.FullDatabaseExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;

@RestController
@RequestMapping("/api/export")
public class ExportController {
    
    @Autowired
    private FullDatabaseExportService fullDatabaseExportService;
    
    @GetMapping("/full-database")
    public ResponseEntity<byte[]> exportFullDatabase() {
        try {
            String csvData = fullDatabaseExportService.exportEntireDatabase();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "complete_database_export.csv");
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvData.getBytes());
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}