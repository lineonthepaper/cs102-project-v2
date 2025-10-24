package com.smartattendance.service;

import com.smartattendance.dto.request.CreateStudentRequest;
import com.smartattendance.dto.response.StudentDTO;
import com.smartattendance.entity.User;
import com.smartattendance.exception.DuplicateEmailException;
import com.smartattendance.exception.UserNotFoundException;
import com.smartattendance.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private SectionEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private com.smartattendance.mapper.EntityMapper mapper;

    @InjectMocks
    private StudentService studentService;

    private User testStudent;

    @BeforeEach
    void setUp() {
        testStudent = new User();
        testStudent.setId("S0000001");
        testStudent.setEmail("test@example.com");
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setIsStudent(true);
        testStudent.setIsInstructor(false);
        testStudent.setIsTA(false);
        testStudent.setEnabled(true);
    }

    @Test
    void getAllStudents_ShouldReturnListOfStudents() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(testStudent));
        when(attendanceRecordRepository.findByUserIdIn(any())).thenReturn(Collections.emptyList());
        when(mapper.toAttendanceRecordDTOs(any())).thenReturn(Collections.emptyList());
        when(mapper.toEnrollmentDTOs(any())).thenReturn(Collections.emptyList());

        // Act
        List<StudentDTO> result = studentService.getAllStudents();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("S0000001", result.get(0).getId());
        assertEquals("test@example.com", result.get(0).getEmail());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllStudents_ShouldReturnEmptyList_WhenNoStudents() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<StudentDTO> result = studentService.getAllStudents();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void createStudent_ShouldThrowException_WhenEmailExists() {
        // Arrange
        CreateStudentRequest request = new CreateStudentRequest(
            "test@example.com", "John", "Doe"
        );
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testStudent));

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            studentService.createStudent(request);
        });
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void updateEnrollments_ShouldThrowException_WhenStudentNotFound() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            studentService.updateEnrollments("INVALID_ID", null);
        });
        verify(userRepository, times(1)).findById("INVALID_ID");
    }
}

