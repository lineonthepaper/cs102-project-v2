package com.smartattendance.controller;

import com.smartattendance.dto.request.export.ExportAsCSVRequest;
import com.smartattendance.dto.request.export.ExportAsXLSXRequest;
import com.smartattendance.service.ExportService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/export-xlsx")
    public ResponseEntity<byte[]> exportExcel(@RequestBody ExportAsXLSXRequest request) {
        if(request == null){
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        String filename =   "Attendance_Data_" + System.currentTimeMillis() + ".xlsx";

        byte[] excelBytes = exportService.exportSectionAsXLSX(request);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        headers.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, headers, org.springframework.http.HttpStatus.OK);

    }

    @PostMapping("/export-csv")
    public ResponseEntity<String> exportCsv(@RequestBody ExportAsCSVRequest request) {
        if(request == null){
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        String filename = "Attendance_Data_" + System.currentTimeMillis() + ".xlsx";

        String csvContent = exportService.exportSectionAsCSV(request);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        
        return new ResponseEntity<>(csvContent, headers, org.springframework.http.HttpStatus.OK);
    }
}