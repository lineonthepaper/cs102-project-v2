package com.smartattendance.service;

import com.smartattendance.dto.response.CourseDTO;
import com.smartattendance.entity.Course;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.repository.CourseRepository;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;
    private CourseDTO testCourseDTO;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCode("CS102");
        testCourse.setTitle("Programming Fundamentals II");
        testCourse.setDescription("Core Java programming");

        testCourseDTO = new CourseDTO();
        testCourseDTO.setId(1L);
        testCourseDTO.setCode("CS102");
        testCourseDTO.setTitle("Programming Fundamentals II");
        testCourseDTO.setDescription("Core Java programming");
    }

    @Test
    void getAllCourses_ShouldReturnListOfCourses() {
        // Arrange
        Course course2 = new Course();
        course2.setId(2L);
        course2.setCode("CS201");
        course2.setTitle("Data Structures");
        course2.setDescription("Advanced data structures");

        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse, course2));

        // Act
        List<CourseDTO> result = courseService.getAllCourses();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("CS102", result.get(0).getCode());
        assertEquals("CS201", result.get(1).getCode());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void getAllCourses_WhenNoCourses_ShouldReturnEmptyList() {
        // Arrange
        when(courseRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<CourseDTO> result = courseService.getAllCourses();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void createCourse_WithValidData_ShouldReturnCreatedCourse() {
        // Arrange
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        // Act
        CourseDTO result = courseService.createCourse(testCourseDTO);

        // Assert
        assertNotNull(result);
        assertEquals("CS102", result.getCode());
        assertEquals("Programming Fundamentals II", result.getTitle());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void updateCourse_WithValidId_ShouldReturnUpdatedCourse() {
        // Arrange
        CourseDTO updateDTO = new CourseDTO();
        updateDTO.setCode("CS102");
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated Description");

        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        // Act
        CourseDTO result = courseService.updateCourse(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(courseRepository, times(1)).findById(1L);
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void updateCourse_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            courseService.updateCourse(999L, testCourseDTO);
        });
        verify(courseRepository, times(1)).findById(999L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void deleteCourse_WithValidId_ShouldDeleteCourse() {
        // Arrange
        doNothing().when(courseRepository).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> courseService.deleteCourse(1L));

        // Assert
        verify(courseRepository, times(1)).deleteById(1L);
    }
}

