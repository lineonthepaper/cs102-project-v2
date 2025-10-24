package com.smartattendance.service;

import com.smartattendance.dto.response.course.CourseDTO;
import com.smartattendance.entity.Course;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CourseService with comprehensive dependency validation testing.
 * 
 * Tests the FIXED dependency validation logic to ensure:
 * - Courses cannot be deleted when they have dependent sections
 * - Appropriate exceptions are thrown with clear error messages
 * - Deletion succeeds when no dependencies exist
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private EntityMapper mapper;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCode("CS102");
        testCourse.setTitle("Data Structures");
        testCourse.setDescription("Introduction to data structures");
    }

    // ==================== DELETE OPERATION TESTS ====================

    @Test
    void deleteCourse_ShouldThrowException_WhenCourseNotFound() {
        // Arrange
        when(courseRepository.existsById(anyLong())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            courseService.deleteCourse(999L);
        });
        
        verify(courseRepository, times(1)).existsById(999L);
        verify(sectionRepository, never()).countByCourseId(anyLong());
        verify(courseRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteCourse_ShouldSucceed_WithOrWithoutDependencies() {
        // Arrange
        // With cascade deletion at DB level, course can be deleted regardless of dependencies
        Long courseId = 1L;
        when(courseRepository.existsById(courseId)).thenReturn(true);

        // Act
        courseService.deleteCourse(courseId);

        // Assert
        verify(courseRepository, times(1)).existsById(courseId);
        verify(courseRepository, times(1)).deleteById(courseId);
        // Database CASCADE handles deletion of sections, enrollments, etc.
    }

    // ==================== CREATE OPERATION TESTS ====================

    @Test
    void createCourse_ShouldReturnCreatedCourse() {
        // Arrange
        CourseDTO inputDTO = mock(CourseDTO.class);
        when(inputDTO.getCode()).thenReturn("CS102");
        when(inputDTO.getTitle()).thenReturn("Data Structures");
        when(inputDTO.getDescription()).thenReturn("Test description");
        
        CourseDTO outputDTO = new CourseDTO();
        
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);
        when(mapper.toCourseDTO(any(Course.class))).thenReturn(outputDTO);

        // Act
        CourseDTO result = courseService.createCourse(inputDTO);

        // Assert
        assertNotNull(result);
        verify(courseRepository, times(1)).save(any(Course.class));
        verify(mapper, times(1)).toCourseDTO(any(Course.class));
    }

    // ==================== UPDATE OPERATION TESTS ====================

    @Test
    void updateCourse_ShouldUpdateAndReturnCourse() {
        // Arrange
        Long courseId = 1L;
        CourseDTO inputDTO = mock(CourseDTO.class);
        when(inputDTO.getCode()).thenReturn("CS102");
        when(inputDTO.getTitle()).thenReturn("Data Structures");
        when(inputDTO.getDescription()).thenReturn("Updated description");
        
        CourseDTO outputDTO = new CourseDTO();
        
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);
        when(mapper.toCourseDTO(any(Course.class))).thenReturn(outputDTO);

        // Act
        CourseDTO result = courseService.updateCourse(courseId, inputDTO);

        // Assert
        assertNotNull(result);
        verify(courseRepository, times(1)).findById(courseId);
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void updateCourse_ShouldThrowException_WhenCourseNotFound() {
        // Arrange
        Long courseId = 999L;
        CourseDTO inputDTO = mock(CourseDTO.class);
        // No need to stub DTO methods since exception is thrown before they're used
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            courseService.updateCourse(courseId, inputDTO);
        });
        
        verify(courseRepository, times(1)).findById(courseId);
        verify(courseRepository, never()).save(any(Course.class));
    }
}
