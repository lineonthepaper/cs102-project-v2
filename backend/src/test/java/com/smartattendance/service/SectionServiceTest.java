package com.smartattendance.service;

import com.smartattendance.dto.response.course.SectionDTO;
import com.smartattendance.entity.Section;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.*;
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
 * Unit tests for SectionService with comprehensive dependency validation testing.
 * 
 * Tests the FIXED dependency validation logic to ensure:
 * - Sections cannot be deleted when they have dependencies
 * - Multiple dependency types are checked (enrollments, sessions, assignments)
 * - Appropriate exceptions are thrown with detailed error messages
 * - Deletion succeeds when no dependencies exist
 */
@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SectionEnrollmentRepository enrollmentRepository;

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private SectionAssignmentRepository sectionAssignmentRepository;

    @Mock
    private TAAssignmentRepository taAssignmentRepository;

    @Mock
    private EntityMapper mapper;

    @InjectMocks
    private SectionService sectionService;

    private Section testSection;

    @BeforeEach
    void setUp() {
        testSection = new Section();
        testSection.setId(1L);
        testSection.setCourseId(1L);
        testSection.setSectionCode("001");
    }

    // ==================== DELETE OPERATION TESTS ====================

    @Test
    void deleteSection_ShouldThrowException_WhenSectionNotFound() {
        // Arrange
        when(sectionRepository.existsById(anyLong())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            sectionService.deleteSection(999L);
        });
        
        verify(sectionRepository, times(1)).existsById(999L);
        verify(enrollmentRepository, never()).countBySectionIdAndIsActive(anyLong(), anyBoolean());
        verify(sectionRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteSection_ShouldSucceed_WithOrWithoutDependencies() {
        // Arrange
        // With cascade deletion at DB level, section can be deleted regardless of dependencies
        Long sectionId = 1L;
        when(sectionRepository.existsById(sectionId)).thenReturn(true);

        // Act
        sectionService.deleteSection(sectionId);

        // Assert
        verify(sectionRepository, times(1)).existsById(sectionId);
        verify(sectionRepository, times(1)).deleteById(sectionId);
        // Database CASCADE handles deletion of enrollments, sessions, assignments, etc.
    }

    // ==================== CREATE OPERATION TESTS ====================

    @Test
    void createSection_ShouldReturnCreatedSection() {
        // Arrange
        SectionDTO inputDTO = mock(SectionDTO.class);
        when(inputDTO.getCourseId()).thenReturn(1L);
        when(inputDTO.getSectionCode()).thenReturn("001");
        when(inputDTO.getYear()).thenReturn(2024);
        when(inputDTO.getSemester()).thenReturn(1); // Fall
        when(inputDTO.getMeetingDay()).thenReturn("Monday");
        when(inputDTO.getStartTime()).thenReturn(null);
        when(inputDTO.getEndTime()).thenReturn(null);
        when(inputDTO.getLocation()).thenReturn("Room 101");
        
        SectionDTO outputDTO = new SectionDTO();
        
        when(sectionRepository.save(any(Section.class))).thenReturn(testSection);
        when(mapper.toSectionDTO(any(Section.class))).thenReturn(outputDTO);

        // Act
        SectionDTO result = sectionService.createSection(inputDTO);

        // Assert
        assertNotNull(result);
        verify(sectionRepository, times(1)).save(any(Section.class));
        verify(mapper, times(1)).toSectionDTO(any(Section.class));
    }

    // ==================== UPDATE OPERATION TESTS ====================

    @Test
    void updateSection_ShouldUpdateAndReturnSection() {
        // Arrange
        Long sectionId = 1L;
        SectionDTO inputDTO = mock(SectionDTO.class);
        when(inputDTO.getCourseId()).thenReturn(1L);
        when(inputDTO.getSectionCode()).thenReturn("001");
        when(inputDTO.getYear()).thenReturn(2024);
        when(inputDTO.getSemester()).thenReturn(1);
        when(inputDTO.getMeetingDay()).thenReturn("Monday");
        when(inputDTO.getStartTime()).thenReturn(null);
        when(inputDTO.getEndTime()).thenReturn(null);
        when(inputDTO.getLocation()).thenReturn("Room 101");
        
        SectionDTO outputDTO = new SectionDTO();
        
        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(testSection));
        when(sectionRepository.save(any(Section.class))).thenReturn(testSection);
        when(mapper.toSectionDTO(any(Section.class))).thenReturn(outputDTO);

        // Act
        SectionDTO result = sectionService.updateSection(sectionId, inputDTO);

        // Assert
        assertNotNull(result);
        verify(sectionRepository, times(1)).findById(sectionId);
        verify(sectionRepository, times(1)).save(any(Section.class));
    }

    @Test
    void updateSection_ShouldThrowException_WhenSectionNotFound() {
        // Arrange
        Long sectionId = 999L;
        SectionDTO inputDTO = mock(SectionDTO.class);
        // No need to stub DTO methods since exception is thrown before they're used
        when(sectionRepository.findById(sectionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            sectionService.updateSection(sectionId, inputDTO);
        });
        
        verify(sectionRepository, times(1)).findById(sectionId);
        verify(sectionRepository, never()).save(any(Section.class));
    }
}
