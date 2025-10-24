package com.smartattendance.service;

import com.smartattendance.dto.request.auth.LoginRequest;
import com.smartattendance.dto.request.auth.RegisterRequest;
import com.smartattendance.dto.response.auth.LoginResponse;
import com.smartattendance.dto.response.auth.UserDTO;
import com.smartattendance.entity.User;
import com.smartattendance.exception.AuthenticationFailedException;
import com.smartattendance.exception.DuplicateEmailException;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.mapper.EntityMapper;
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
    private final EntityMapper mapper;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service.role.key}")
    private String supabaseServiceRoleKey;

    /**
     * Constructor with dependency injection for better testability and adherence to DIP.
     * RestTemplate and EntityMapper are now injected rather than directly instantiated.
     * 
     * @param userRepository the user repository
     * @param restTemplate the REST template for HTTP requests
     * @param mapper the entity mapper for DTO conversions
     */
    public AuthService(UserRepository userRepository, RestTemplate restTemplate, EntityMapper mapper) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.mapper = mapper;
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
                        return new ResourceNotFoundException("User", "authenticated user");
                    });
            
            UserDTO userDTO = mapToUserDTO(user);
            
            logger.info("Login successful for user: {} (ID: {})", request.getEmail(), user.getId());
            return new LoginResponse(accessToken, refreshToken, userDTO);
        } catch (AuthenticationFailedException | ResourceNotFoundException e) {
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
                    .orElseThrow(() -> new ResourceNotFoundException("User", generatedId));
            
            logger.info("Registration successful for: {} {} ({})", request.getFirstName(), request.getLastName(), request.getEmail());
            return mapToUserDTO(savedUser);
        } catch (DuplicateEmailException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Registration failed for: {}", request.getEmail(), e);
            throw new AuthenticationFailedException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Map User to UserDTO.
     * 
     * REFACTORED: Now delegates to EntityMapper (DRY principle).
     * Before: Manual DTO construction (12 lines of duplicate code)
     * After: Single method call (1 line)
     * 
     * This eliminates code duplication and ensures consistency across the application.
     */
    private UserDTO mapToUserDTO(User user) {
        return mapper.toUserDTO(user);
    }
}

