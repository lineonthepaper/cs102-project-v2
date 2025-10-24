package com.smartattendance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

