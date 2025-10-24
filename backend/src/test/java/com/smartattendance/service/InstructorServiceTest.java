package com.smartattendance.service;

import com.smartattendance.dto.request.user.AddInstructorRequest;
import com.smartattendance.dto.request.user.UpdateInstructorAssignmentsRequest;
import com.smartattendance.dto.response.user.InstructorDTO;
import com.smartattendance.entity.SectionAssignment;
import com.smartattendance.entity.User;
import com.smartattendance.repository.SectionAssignmentRepository;
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
class InstructorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SectionAssignmentRepository sectionAssignmentRepository;

    @InjectMocks
    private InstructorService instructorService;

    private User testInstructor;
    private SectionAssignment testAssignment;

    @BeforeEach
    void setUp() {
        testInstructor = new User();
        testInstructor.setId("I0000001");
        testInstructor.setAuthId("auth-instructor-123");
        testInstructor.setEmail("instructor@test.com");
        testInstructor.setFirstName("Prof");
        testInstructor.setLastName("Smith");
        testInstructor.setIsInstructor(true);
        testInstructor.setIsTA(false);
        testInstructor.setIsStudent(false);

        testAssignment = new SectionAssignment();
        testAssignment.setId(1L);
        testAssignment.setUserId("I0000001");
        testAssignment.setSectionId(1L);
    }

    @Test
    void getAllInstructors_ShouldReturnListOfInstructors() {
        // Arrange
        User instructor2 = new User();
        instructor2.setId("I0000002");
        instructor2.setEmail("prof2@test.com");
        instructor2.setFirstName("Dr");
        instructor2.setLastName("Jones");
        instructor2.setIsInstructor(true);

        when(userRepository.findByIsInstructorTrue()).thenReturn(Arrays.asList(testInstructor, instructor2));
        when(sectionAssignmentRepository.findByUserIdAndRoleAndIsActive("I0000001", "INSTRUCTOR", true))
                .thenReturn(Arrays.asList(testAssignment));
        when(sectionAssignmentRepository.findByUserIdAndRoleAndIsActive("I0000002", "INSTRUCTOR", true))
                .thenReturn(Arrays.asList());

        // Act
        List<InstructorDTO> result = instructorService.getAllInstructors();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("I0000001", result.get(0).getId());
        assertEquals("instructor@test.com", result.get(0).getEmail());
        verify(userRepository, times(1)).findByIsInstructorTrue();
    }

    @Test
    void getAllInstructors_WhenNoInstructors_ShouldReturnEmptyList() {
        // Arrange
        when(userRepository.findByIsInstructorTrue()).thenReturn(Arrays.asList());

        // Act
        List<InstructorDTO> result = instructorService.getAllInstructors();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findByIsInstructorTrue();
    }

    @Test
    void addInstructor_WithExistingUser_ShouldUpdateToInstructor() {
        // Arrange
        AddInstructorRequest request = new AddInstructorRequest();
        request.setEmail("existing@test.com");
        request.setFirstName("Updated");
        request.setLastName("Professor");

        User existingUser = new User();
        existingUser.setId("S0000001");
        existingUser.setEmail("existing@test.com");
        existingUser.setFirstName("Old");
        existingUser.setLastName("Name");
        existingUser.setIsStudent(true);
        existingUser.setIsInstructor(false);

        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        InstructorDTO result = instructorService.addInstructor(request);

        // Assert
        assertNotNull(result);
        assertEquals("S0000001", result.getId());
        verify(userRepository, times(1)).findByEmail("existing@test.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateInstructorAssignments_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        UpdateInstructorAssignmentsRequest request = new UpdateInstructorAssignmentsRequest();
        request.setSectionIds(Arrays.asList(1L, 2L, 3L));

        when(userRepository.findById("I0000001")).thenReturn(Optional.of(testInstructor));
        doNothing().when(sectionAssignmentRepository).deleteByUserIdAndRole("I0000001", "INSTRUCTOR");
        when(sectionAssignmentRepository.saveAll(anyList())).thenReturn(Arrays.asList(testAssignment));

        // Act
        assertDoesNotThrow(() -> instructorService.updateInstructorAssignments("I0000001", request));

        // Assert
        verify(userRepository, times(1)).findById("I0000001");
        verify(sectionAssignmentRepository, times(1)).deleteByUserIdAndRole("I0000001", "INSTRUCTOR");
        verify(sectionAssignmentRepository, times(1)).saveAll(anyList());
    }

    @Test
    void updateInstructorAssignments_WithEmptyList_ShouldDeleteAllAssignments() {
        // Arrange
        UpdateInstructorAssignmentsRequest request = new UpdateInstructorAssignmentsRequest();
        request.setSectionIds(Arrays.asList());

        when(userRepository.findById("I0000001")).thenReturn(Optional.of(testInstructor));
        doNothing().when(sectionAssignmentRepository).deleteByUserIdAndRole("I0000001", "INSTRUCTOR");

        // Act
        assertDoesNotThrow(() -> instructorService.updateInstructorAssignments("I0000001", request));

        // Assert
        verify(userRepository, times(1)).findById("I0000001");
        verify(sectionAssignmentRepository, times(1)).deleteByUserIdAndRole("I0000001", "INSTRUCTOR");
        verify(sectionAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void removeInstructor_WithValidId_ShouldDeleteInstructorAndAssignments() {
        // Arrange
        when(userRepository.findById("I0000001")).thenReturn(Optional.of(testInstructor));
        doNothing().when(userRepository).delete(testInstructor);
        doNothing().when(sectionAssignmentRepository).deleteByUserIdAndRole("I0000001", "INSTRUCTOR");

        // Act
        assertDoesNotThrow(() -> instructorService.removeInstructor("I0000001"));

        // Assert
        verify(userRepository, times(1)).findById("I0000001");
        verify(userRepository, times(1)).delete(testInstructor);
        verify(sectionAssignmentRepository, times(1)).deleteByUserIdAndRole("I0000001", "INSTRUCTOR");
    }

    @Test
    void removeInstructor_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(userRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            instructorService.removeInstructor("INVALID");
        });
        verify(userRepository, times(1)).findById("INVALID");
        verify(userRepository, never()).delete(any());
    }
}

