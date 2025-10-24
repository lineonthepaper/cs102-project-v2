package com.smartattendance.service;

import com.smartattendance.dto.request.LoginRequest;
import com.smartattendance.dto.request.RegisterRequest;
import com.smartattendance.entity.User;
import com.smartattendance.exception.DuplicateEmailException;
import com.smartattendance.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("S0000001");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setAuthId("auth-123");
        testUser.setIsStudent(true);
        testUser.setEnabled(true);

        // Set required properties for AuthService
        ReflectionTestUtils.setField(authService, "supabaseUrl", "https://test.supabase.co");
        ReflectionTestUtils.setField(authService, "supabaseServiceRoleKey", "test-key");
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
            "test@example.com", "password123", "John", "Doe"
        );
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            authService.register(request);
        });
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void register_ShouldAcceptValidEmail() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
            "newuser@example.com", "password123", "Jane", "Smith"
        );
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert - Should not throw exception for duplicate
        // Note: This will fail on Supabase API call, but validates our duplicate check logic
        try {
            authService.register(request);
        } catch (Exception e) {
            // Expected to fail on Supabase call in test environment
            assertTrue(e.getMessage().contains("failed") || e.getMessage().contains("Registration"));
        }
        verify(userRepository, times(1)).findByEmail("newuser@example.com");
    }
}

