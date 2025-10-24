package com.smartattendance.service;

import com.smartattendance.dto.request.user.AddTARequest;
import com.smartattendance.dto.request.user.UpdateTAAssignmentsRequest;
import com.smartattendance.dto.response.user.TADTO;
import com.smartattendance.entity.TAAssignment;
import com.smartattendance.entity.User;
import com.smartattendance.repository.TAAssignmentRepository;
import com.smartattendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TAServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TAAssignmentRepository taAssignmentRepository;

    @InjectMocks
    private TAService taService;

    private User testTA;
    private TAAssignment testAssignment;

    @BeforeEach
    void setUp() {
        testTA = new User();
        testTA.setId("T0000001");
        testTA.setAuthId("auth-123");
        testTA.setEmail("ta@test.com");
        testTA.setFirstName("John");
        testTA.setLastName("Doe");
        testTA.setIsTA(true);
        testTA.setIsInstructor(false);
        testTA.setIsStudent(false);

        testAssignment = new TAAssignment();
        testAssignment.setId(1L);
        testAssignment.setUserId("T0000001");
        testAssignment.setSectionId(1L);
    }

    @Test
    void getAllTAs_ShouldReturnListOfTAs() {
        // Arrange
        User ta2 = new User();
        ta2.setId("T0000002");
        ta2.setEmail("ta2@test.com");
        ta2.setFirstName("Jane");
        ta2.setLastName("Smith");
        ta2.setIsTA(true);

        when(userRepository.findByIsTATrue()).thenReturn(Arrays.asList(testTA, ta2));
        when(taAssignmentRepository.findByUserId("T0000001")).thenReturn(Arrays.asList(testAssignment));
        when(taAssignmentRepository.findByUserId("T0000002")).thenReturn(Arrays.asList());

        // Act
        List<TADTO> result = taService.getAllTAs();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("T0000001", result.get(0).getId());
        assertEquals("ta@test.com", result.get(0).getEmail());
        verify(userRepository, times(1)).findByIsTATrue();
    }

    @Test
    void getAllTAs_WhenNoTAs_ShouldReturnEmptyList() {
        // Arrange
        when(userRepository.findByIsTATrue()).thenReturn(Arrays.asList());

        // Act
        List<TADTO> result = taService.getAllTAs();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findByIsTATrue();
    }

    @Test
    void addTA_WithValidStudent_ShouldReturnCreatedTA() {
        // Arrange
        AddTARequest request = new AddTARequest();
        request.setEmail("student@test.com");
        request.setType("student");

        User student = new User();
        student.setId("S0000001");
        student.setEmail("student@test.com");
        student.setFirstName("Student");
        student.setLastName("User");
        student.setIsStudent(true);
        student.setIsTA(false);

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);

        // Act
        TADTO result = taService.addTA(request);

        // Assert
        assertNotNull(result);
        assertEquals("S0000001", result.getId());
        verify(userRepository, times(1)).findByEmail("student@test.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void addTA_WithNonExistentEmail_ShouldThrowException() {
        // Arrange
        AddTARequest request = new AddTARequest();
        request.setEmail("invalid@test.com");
        request.setType("student");

        when(userRepository.findByEmail("invalid@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            taService.addTA(request);
        });
        verify(userRepository, times(1)).findByEmail("invalid@test.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateTAAssignments_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        UpdateTAAssignmentsRequest request = new UpdateTAAssignmentsRequest();
        request.setSectionIds(Arrays.asList(1L, 2L));

        when(userRepository.findById("T0000001")).thenReturn(Optional.of(testTA));
        doNothing().when(taAssignmentRepository).deleteByUserId("T0000001");
        when(taAssignmentRepository.saveAll(anyList())).thenReturn(Arrays.asList(testAssignment));

        // Act
        assertDoesNotThrow(() -> taService.updateTAAssignments("T0000001", request));

        // Assert
        verify(userRepository, times(1)).findById("T0000001");
        verify(taAssignmentRepository, times(1)).deleteByUserId("T0000001");
        verify(taAssignmentRepository, times(1)).saveAll(anyList());
    }

    @Test
    void removeTA_WithValidId_ShouldSetIsTAToFalse() {
        // Arrange
        when(userRepository.findById("T0000001")).thenReturn(Optional.of(testTA));
        when(userRepository.save(any(User.class))).thenReturn(testTA);
        doNothing().when(taAssignmentRepository).deleteByUserId("T0000001");

        // Act
        assertDoesNotThrow(() -> taService.removeTA("T0000001"));

        // Assert
        verify(userRepository, times(1)).findById("T0000001");
        verify(userRepository, times(1)).save(any(User.class));
        verify(taAssignmentRepository, times(1)).deleteByUserId("T0000001");
    }

    @Test
    void removeTA_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(userRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            taService.removeTA("INVALID");
        });
        verify(userRepository, times(1)).findById("INVALID");
        verify(taAssignmentRepository, never()).deleteByUserId(any());
    }
}

