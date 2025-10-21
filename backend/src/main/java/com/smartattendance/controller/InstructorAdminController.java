package com.smartattendance.controller;

import com.smartattendance.service.SupabaseAuthService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instructors")
@CrossOrigin(origins = "http://localhost:5173")
public class InstructorAdminController {

    private final SupabaseAuthService supabaseAuthService;

    public InstructorAdminController(SupabaseAuthService supabaseAuthService) {
        this.supabaseAuthService = supabaseAuthService;
    }

    @DeleteMapping("/{authId}")
    public ResponseEntity<Map<String, Object>> removeInstructorAuth(@PathVariable String authId) {
        supabaseAuthService.removeAuthUser(authId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("authId", authId);
        response.put("message", "Instructor removed from Supabase Auth");

        return ResponseEntity.ok(response);
    }
}

