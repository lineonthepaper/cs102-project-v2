package com.smartattendance.service;

import com.smartattendance.dto.request.LoginRequest;
import com.smartattendance.dto.request.RegisterRequest;
import com.smartattendance.dto.response.LoginResponse;
import com.smartattendance.dto.response.UserDTO;
import com.smartattendance.entity.User;
import com.smartattendance.exception.AuthenticationFailedException;
import com.smartattendance.exception.DuplicateEmailException;
import com.smartattendance.exception.UserNotFoundException;
import com.smartattendance.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

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
        logger.info("Login attempt for user: {}", request.getEmail());
        
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
                logger.error("Authentication failed for user: {} - Empty response from Supabase", request.getEmail());
                throw new AuthenticationFailedException("Authentication failed - Invalid response");
            }
            
            String accessToken = (String) responseBody.get("access_token");
            String refreshToken = (String) responseBody.get("refresh_token");
            Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");
            String authId = (String) userMap.get("id");
            
            logger.debug("Supabase authentication successful for auth ID: {}", authId);
            
            // Fetch user from database
            User user = userRepository.findByAuthId(authId)
                    .orElseThrow(() -> {
                        logger.error("User authenticated with Supabase but not found in database: {}", authId);
                        return new UserNotFoundException("User authenticated but not found in local database");
                    });
            
            UserDTO userDTO = mapToUserDTO(user);
            
            logger.info("Login successful for user: {} (ID: {})", request.getEmail(), user.getId());
            return new LoginResponse(accessToken, refreshToken, userDTO);
        } catch (AuthenticationFailedException | UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Login failed for user: {}", request.getEmail(), e);
            throw new AuthenticationFailedException("Login failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        logger.info("Registration attempt for email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Registration failed - duplicate email: {}", request.getEmail());
            throw new DuplicateEmailException("User with email " + request.getEmail() + " already exists");
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
                logger.error("Registration failed for {} - Empty response from Supabase", request.getEmail());
                throw new AuthenticationFailedException("Registration failed - Invalid response");
            }
            
            Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");
            String authId = (String) userMap.get("id");
            
            logger.debug("Supabase user created with auth ID: {}", authId);
            
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
            
            logger.debug("Database user created with ID: {}", generatedId);
            
            // Fetch the newly created user
            User savedUser = userRepository.findById(generatedId)
                    .orElseThrow(() -> new UserNotFoundException("Failed to fetch created user"));
            
            logger.info("Registration successful for: {} {} ({})", request.getFirstName(), request.getLastName(), request.getEmail());
            return mapToUserDTO(savedUser);
        } catch (DuplicateEmailException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Registration failed for: {}", request.getEmail(), e);
            throw new AuthenticationFailedException("Registration failed: " + e.getMessage(), e);
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

