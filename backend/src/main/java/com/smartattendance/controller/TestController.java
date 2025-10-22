package com.smartattendance.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class TestController {

    @GetMapping("/test")
    public String testConnection() {
        return "Backend is connected successfully!";
    }

    @GetMapping("/users")
    public String testUserConnection() {
        return "User database connection test successful!";
    }
}