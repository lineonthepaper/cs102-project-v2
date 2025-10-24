package com.smartattendance.service;

import com.smartattendance.dto.response.course.CourseDTO;
import com.smartattendance.entity.Course;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.SectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Course management.
 * Handles business logic and dependency validation.
 */
@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);
    
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final EntityMapper mapper;

    public CourseService(CourseRepository courseRepository, SectionRepository sectionRepository, EntityMapper mapper) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        return mapper.toCourseDTOs(courseRepository.findAll());
    }

    @Transactional
    public CourseDTO createCourse(CourseDTO courseDTO) {
        Course course = new Course();
        course.setCode(courseDTO.getCode());
        course.setTitle(courseDTO.getTitle());
        course.setDescription(courseDTO.getDescription());
        
        return mapper.toCourseDTO(courseRepository.save(course));
    }

    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id.toString()));
        
        course.setCode(courseDTO.getCode());
        course.setTitle(courseDTO.getTitle());
        course.setDescription(courseDTO.getDescription());
        
        return mapper.toCourseDTO(courseRepository.save(course));
    }

    /**
     * Delete a course.
     * 
     * Cascade deletion is handled by database foreign key constraints (ON DELETE CASCADE).
     * The database automatically deletes all related records in the correct order.
     * 
     * @param id the course ID to delete
     * @throws ResourceNotFoundException if course doesn't exist
     */
    @Transactional
    public void deleteCourse(Long id) {
        logger.info("Deleting course with ID: {}", id);
        
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course", id.toString());
        }
        
        courseRepository.deleteById(id);
        logger.info("Successfully deleted course {}", id);
    }
}

