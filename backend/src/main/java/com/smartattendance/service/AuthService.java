package com.smartattendance.service;

import com.smartattendance.dto.request.LoginRequest;
import com.smartattendance.dto.request.RegisterRequest;
import com.smartattendance.dto.response.LoginResponse;
import com.smartattendance.dto.response.UserDTO;
import com.smartattendance.entity.User;
import com.smartattendance.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service.role.key}")
    private String supabaseServiceRoleKey;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Call Supabase Auth API to authenticate
        String url = supabaseUrl + "/auth/v1/token?grant_type=password";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseServiceRoleKey);
        
        Map<String, String> body = new HashMap<>();
        body.put("email", request.getEmail());
        body.put("password", request.getPassword());
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null) {
                throw new RuntimeException("Authentication failed");
            }
            
            String accessToken = (String) responseBody.get("access_token");
            String refreshToken = (String) responseBody.get("refresh_token");
            Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");
            String authId = (String) userMap.get("id");
            
            // Fetch user from database
            User user = userRepository.findByAuthId(authId)
                    .orElseThrow(() -> new RuntimeException("User not found in database"));
            
            UserDTO userDTO = mapToUserDTO(user);
            
            return new LoginResponse(accessToken, refreshToken, userDTO);
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        
        // Create user in Supabase Auth
        String url = supabaseUrl + "/auth/v1/signup";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseServiceRoleKey);
        
        Map<String, String> body = new HashMap<>();
        body.put("email", request.getEmail());
        body.put("password", request.getPassword());
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null) {
                throw new RuntimeException("Registration failed");
            }
            
            Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");
            String authId = (String) userMap.get("id");
            
            // Create user in database using native SQL
            String sql = "INSERT INTO users (email, first_name, last_name, is_student, is_instructor, is_ta, enabled, auth_id, created_at) " +
                         "VALUES (:email, :firstName, :lastName, true, false, false, true, :authId, CURRENT_TIMESTAMP) " +
                         "RETURNING id";
            
            String generatedId = (String) entityManager.createNativeQuery(sql)
                    .setParameter("email", request.getEmail())
                    .setParameter("firstName", request.getFirstName())
                    .setParameter("lastName", request.getLastName())
                    .setParameter("authId", authId)
                    .getSingleResult();
            
            // Fetch the newly created user
            User savedUser = userRepository.findById(generatedId)
                    .orElseThrow(() -> new RuntimeException("Failed to fetch created user"));
            
            return mapToUserDTO(savedUser);
        } catch (Exception e) {
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    private UserDTO mapToUserDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getIsStudent(),
                user.getIsInstructor(),
                user.getIsTA(),
                user.getEnabled(),
                user.getAuthId()
        );
    }
}

