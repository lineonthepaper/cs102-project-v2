package com.smartattendance.dto.common;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    
    public ErrorResponse(String error, String message) {
        this.timestamp = LocalDateTime.now();
        this.error = error;
        this.message = message;
    }
}