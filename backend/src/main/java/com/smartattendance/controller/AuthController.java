package com.smartattendance.controller;

import com.smartattendance.dto.request.auth.LoginRequest;
import com.smartattendance.dto.request.auth.RegisterRequest;
import com.smartattendance.dto.response.auth.LoginResponse;
import com.smartattendance.dto.response.auth.UserDTO;
import com.smartattendance.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for: {}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received for: {}", request.getEmail());
        UserDTO user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}

