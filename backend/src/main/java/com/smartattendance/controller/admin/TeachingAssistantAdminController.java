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
@RequestMapping("/api/admin/teaching-assistants")
@CrossOrigin(origins = "http://localhost:5173")
public class TeachingAssistantAdminController {

    private final SupabaseAuthService supabaseAuthService;

    public TeachingAssistantAdminController(SupabaseAuthService supabaseAuthService) {
        this.supabaseAuthService = supabaseAuthService;
    }

    @DeleteMapping("/{authId}")
    public ResponseEntity<Map<String, Object>> removeTeachingAssistantAuth(@PathVariable String authId) {
        supabaseAuthService.removeAuthUser(authId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("authId", authId);
        response.put("message", "Teaching Assistant removed from Supabase Auth");

        return ResponseEntity.ok(response);
    }
}
