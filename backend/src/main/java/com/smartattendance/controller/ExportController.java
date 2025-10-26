package com.smartattendance.controller;

import com.smartattendance.entity.User;  
import com.smartattendance.service.CSVExportService;
import com.smartattendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.List; 

@RestController
@RequestMapping("/api/export")
public class ExportController {
    
    @Autowired
    private CSVExportService csvExportService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/users")
    public ResponseEntity<String> exportUsersCSV() {
        try {
            List<User> users = userRepository.findAll();
            String csv = csvExportService.generateCSV(users);  // ← Changed method name
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "users.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}