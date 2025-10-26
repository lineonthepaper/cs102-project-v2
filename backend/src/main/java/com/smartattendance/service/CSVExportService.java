package com.smartattendance.service;

import com.smartattendance.entity.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import java.io.StringWriter;
import java.io.IOException;
import java.util.List;

@Service
public class CSVExportService {
    
    public String generateCSV(List<User> users) throws IOException {  // ← Changed method name
        StringWriter writer = new StringWriter();
        
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "Email", "First Name", "Last Name", "Role", 
                          "Is Student", "Is Instructor", "Is TA", "Enabled", 
                          "Created At", "Updated At")
                .build();
        
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
            for (User user : users) {
                csvPrinter.printRecord(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole(),
                    user.getIsStudent(),
                    user.getIsInstructor(),
                    user.getIsTA(),
                    user.getEnabled(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
                );
            }
        }
        
        return writer.toString();
    }
}