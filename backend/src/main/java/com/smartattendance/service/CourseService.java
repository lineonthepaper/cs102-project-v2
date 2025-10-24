package com.smartattendance.service;

import com.smartattendance.dto.response.course.CourseDTO;
import com.smartattendance.entity.Course;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final EntityMapper mapper;

    public CourseService(CourseRepository courseRepository, EntityMapper mapper) {
        this.courseRepository = courseRepository;
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
                .orElseThrow(() -> new com.smartattendance.exception.ResourceNotFoundException("Course", id.toString()));
        
        course.setCode(courseDTO.getCode());
        course.setTitle(courseDTO.getTitle());
        course.setDescription(courseDTO.getDescription());
        
        return mapper.toCourseDTO(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}

