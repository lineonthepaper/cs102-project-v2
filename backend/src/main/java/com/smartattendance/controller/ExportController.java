package com.smartattendance.controller;

import com.smartattendance.service.ExportService;
import com.smartattendance.service.FullDatabaseExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;

@CrossOrigin(origins = "http://localhost:5173") 
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final FullDatabaseExportService fullDatabaseExportService;
    private final ExportService exportService;

    public ExportController(FullDatabaseExportService fullDatabaseExportService, ExportService exportService) {
        this.fullDatabaseExportService = fullDatabaseExportService;
        this.exportService = exportService;
    }

    @GetMapping("/full-database-zip")
    public ResponseEntity<byte[]> exportFullDatabaseAsZip() {
        try {
            byte[] zipData = fullDatabaseExportService.exportEntireDatabaseAsZip();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", "complete_database_export.zip");
            headers.setCacheControl("no-cache, no-store, must-revalidate");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/test")
    public void testEndpoint() {
        exportService.exportSectionAsXLSX(1);
    }
}