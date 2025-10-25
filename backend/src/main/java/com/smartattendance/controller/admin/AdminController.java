package com.smartattendance.controller.admin;

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
@RequestMapping("/api/admin/")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final SupabaseAuthService supabaseAuthService;

    public AdminController(SupabaseAuthService supabaseAuthService) {
        this.supabaseAuthService = supabaseAuthService;
    }

    @DeleteMapping("/instructors/{authId}")
    public ResponseEntity<Map<String, Object>> removeInstructorAuth(@PathVariable String authId) {
        supabaseAuthService.removeAuthUser(authId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("authId", authId);
        response.put("message", "Instructor removed from Supabase Auth");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/teaching-assistants/{authId}")
    public ResponseEntity<Map<String, Object>> removeTeachingAssistantAuth(@PathVariable String authId) {
        supabaseAuthService.removeAuthUser(authId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("authId", authId);
        response.put("message", "Teaching Assistant removed from Supabase Auth");

        return ResponseEntity.ok(response);
    }
}

