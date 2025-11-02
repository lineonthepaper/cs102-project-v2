package com.smartattendance.service;

import com.smartattendance.dto.response.course.SectionDTO;
import com.smartattendance.dto.response.user.StudentDTO;
import com.smartattendance.entity.Section;
import com.smartattendance.entity.SectionEnrollment;
import com.smartattendance.entity.Semester;
import com.smartattendance.entity.User;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.*;
import com.smartattendance.util.helper.DateTimeUtils;

import lombok.AllArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Section management.
 * Handles business logic and comprehensive dependency validation.
 */
@Service
@AllArgsConstructor
public class SectionService {

    private static final Logger logger = LoggerFactory.getLogger(SectionService.class);

    private final SectionRepository sectionRepository;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<SectionDTO> getAllSections() {
        return mapper.toSectionDTOs(sectionRepository.findAll());
    }

    @Transactional
    public SectionDTO createSection(SectionDTO sectionDTO) {
        Section section = new Section();
        section.setCourseId(sectionDTO.getCourseId());
        section.setSectionCode(sectionDTO.getSectionCode());
        section.setYear(sectionDTO.getYear());
        // Convert Integer to Semester enum
        section.setSemester(sectionDTO.getSemester() != null ? Semester.fromValue(sectionDTO.getSemester()) : null);
        section.setMeetingDay(DateTimeUtils.dayNameToNumber(sectionDTO.getMeetingDay()));
        section.setStartTime(sectionDTO.getStartTime());
        section.setEndTime(sectionDTO.getEndTime());
        section.setLocation(sectionDTO.getLocation());
        
        return mapper.toSectionDTO(sectionRepository.save(section));
    }

    @Transactional
    public SectionDTO updateSection(Long id, SectionDTO sectionDTO) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", id.toString()));
        
        section.setCourseId(sectionDTO.getCourseId());
        section.setSectionCode(sectionDTO.getSectionCode());
        section.setYear(sectionDTO.getYear());
        // Convert Integer to Semester enum
        section.setSemester(sectionDTO.getSemester() != null ? Semester.fromValue(sectionDTO.getSemester()) : null);
        section.setMeetingDay(DateTimeUtils.dayNameToNumber(sectionDTO.getMeetingDay()));
        section.setStartTime(sectionDTO.getStartTime());
        section.setEndTime(sectionDTO.getEndTime());
        section.setLocation(sectionDTO.getLocation());
        
        return mapper.toSectionDTO(sectionRepository.save(section));
    }

    /**
     * Delete a section.
     * 
     * Cascade deletion is handled by database foreign key constraints (ON DELETE CASCADE).
     * The database automatically deletes all related records in the correct order.
     * 
     * @param id the section ID to delete
     * @throws ResourceNotFoundException if section doesn't exist
     */
    @Transactional
    public void deleteSection(Long id) {
        logger.info("Deleting section with ID: {}", id);
        
        if (!sectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Section", id.toString());
        }
        
        sectionRepository.deleteById(id);
        logger.info("Successfully deleted section {}", id);
    }

    /**
     * Get students enrolled in a specific section.
     * Returns minimal student data (no full attendance history) for performance.
     * 
     * @param sectionId the section ID
     * @return list of students enrolled in the section
     * @throws ResourceNotFoundException if section doesn't exist
     */
    @Transactional(readOnly = true)
    public List<StudentDTO> getStudentsBySection(Long sectionId) {
        logger.debug("Fetching students for section ID: {}", sectionId);
        
        // Verify section exists
        if (!sectionRepository.existsById(sectionId)) {
            throw new ResourceNotFoundException("Section", sectionId.toString());
        }
        
        // Get active enrollments for this section
        List<String> studentIds = enrollmentRepository.findBySectionIdAndIsActive(sectionId, true).stream()
            .map(SectionEnrollment::getUserId)
            .collect(Collectors.toList());
        
        if (studentIds.isEmpty()) {
            logger.debug("No students found for section {}", sectionId);
            return Collections.emptyList();
        }
        
        // Fetch students - using findAllById is more efficient than filtering findAll
        List<User> students = userRepository.findAllById(studentIds);
        
        logger.debug("Found {} students for section {}", students.size(), sectionId);
        
        // Map to minimal DTO (no attendance history for performance)
        return students.stream()
            .map(this::mapToMinimalStudentDTO)
            .collect(Collectors.toList());
    }

    /**
     * Map User to minimal StudentDTO without attendance history.
     * Used for performance-critical endpoints like attendance marking.
     */
    private StudentDTO mapToMinimalStudentDTO(User student) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setEmail(student.getEmail());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEnabled(student.getEnabled());
        // Don't include attendance records or enrollments for performance
        return dto;
    }
}

