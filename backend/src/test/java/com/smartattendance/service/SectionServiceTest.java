package com.smartattendance.service;

import com.smartattendance.dto.response.SectionDTO;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Section;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private SectionService sectionService;

    private Section testSection;
    private Course testCourse;
    private SectionDTO testSectionDTO;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCode("CS102");
        testCourse.setTitle("Programming Fundamentals II");

        testSection = new Section();
        testSection.setId(1L);
        testSection.setCourse(testCourse);
        testSection.setSectionCode("CS102-01");
        testSection.setYear(2025);
        testSection.setSemester(1);
        testSection.setMeetingDay(1); // Monday
        testSection.setStartTime(LocalTime.of(9, 0));
        testSection.setEndTime(LocalTime.of(10, 30));
        testSection.setLocation("Room 102");

        testSectionDTO = new SectionDTO();
        testSectionDTO.setId(1L);
        testSectionDTO.setCourseId(1L);
        testSectionDTO.setSectionCode("CS102-01");
        testSectionDTO.setYear(2025);
        testSectionDTO.setSemester(1);
        testSectionDTO.setMeetingDay("Monday");
        testSectionDTO.setStartTime(LocalTime.of(9, 0));
        testSectionDTO.setEndTime(LocalTime.of(10, 30));
        testSectionDTO.setLocation("Room 102");
    }

    @Test
    void getAllSections_ShouldReturnListOfSections() {
        // Arrange
        Section section2 = new Section();
        section2.setId(2L);
        section2.setCourse(testCourse);
        section2.setSectionCode("CS102-02");
        section2.setYear(2025);
        section2.setSemester(1);
        section2.setMeetingDay(3); // Wednesday

        when(sectionRepository.findAll()).thenReturn(Arrays.asList(testSection, section2));

        // Act
        List<SectionDTO> result = sectionService.getAllSections();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("CS102-01", result.get(0).getSectionCode());
        assertEquals("Monday", result.get(0).getMeetingDay());
        assertEquals("CS102-02", result.get(1).getSectionCode());
        verify(sectionRepository, times(1)).findAll();
    }

    @Test
    void createSection_WithValidData_ShouldReturnCreatedSection() {
        // Arrange
        when(sectionRepository.save(any(Section.class))).thenReturn(testSection);

        // Act
        SectionDTO result = sectionService.createSection(testSectionDTO);

        // Assert
        assertNotNull(result);
        assertEquals("CS102-01", result.getSectionCode());
        assertEquals(2025, result.getYear());
        assertEquals(1, result.getSemester());
        assertEquals("Monday", result.getMeetingDay());
        verify(sectionRepository, times(1)).save(any(Section.class));
    }

    @Test
    void createSection_WithDifferentDay_ShouldConvertDayCorrectly() {
        // Arrange
        testSectionDTO.setMeetingDay("Friday");
        Section fridaySection = new Section();
        fridaySection.setId(2L);
        fridaySection.setCourse(testCourse);
        fridaySection.setSectionCode("CS102-02");
        fridaySection.setMeetingDay(5); // Friday
        
        when(sectionRepository.save(any(Section.class))).thenReturn(fridaySection);

        // Act
        SectionDTO result = sectionService.createSection(testSectionDTO);

        // Assert
        assertNotNull(result);
        verify(sectionRepository, times(1)).save(any(Section.class));
    }

    @Test
    void updateSection_WithValidId_ShouldReturnUpdatedSection() {
        // Arrange
        SectionDTO updateDTO = new SectionDTO();
        updateDTO.setCourseId(1L);
        updateDTO.setSectionCode("CS102-01-UPDATED");
        updateDTO.setYear(2025);
        updateDTO.setSemester(2);
        updateDTO.setMeetingDay("Tuesday");
        updateDTO.setLocation("Room 201");

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(sectionRepository.save(any(Section.class))).thenReturn(testSection);

        // Act
        SectionDTO result = sectionService.updateSection(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(sectionRepository, times(1)).findById(1L);
        verify(sectionRepository, times(1)).save(any(Section.class));
    }

    @Test
    void updateSection_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            sectionService.updateSection(999L, testSectionDTO);
        });
        verify(sectionRepository, times(1)).findById(999L);
        verify(sectionRepository, never()).save(any(Section.class));
    }

    @Test
    void deleteSection_WithValidId_ShouldDeleteSection() {
        // Arrange
        doNothing().when(sectionRepository).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> sectionService.deleteSection(1L));

        // Assert
        verify(sectionRepository, times(1)).deleteById(1L);
    }

    @Test
    void dayNameToNumber_ShouldConvertCorrectly() {
        // This tests the helper method indirectly through createSection
        testSectionDTO.setMeetingDay("Wednesday");
        Section wednesdaySection = new Section();
        wednesdaySection.setId(3L);
        wednesdaySection.setMeetingDay(3); // Wednesday
        
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> {
            Section section = invocation.getArgument(0);
            // Verify the day was converted to number 3 (Wednesday)
            assertEquals(3, section.getMeetingDay());
            return wednesdaySection;
        });

        sectionService.createSection(testSectionDTO);
        
        verify(sectionRepository, times(1)).save(any(Section.class));
    }
}

