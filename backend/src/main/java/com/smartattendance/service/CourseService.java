package com.smartattendance.service;

import com.smartattendance.dto.response.CourseDTO;
import com.smartattendance.entity.Course;
import com.smartattendance.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .<CourseDTO>map(course -> new CourseDTO(course.getId(), course.getCode(), course.getTitle(), course.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseDTO createCourse(CourseDTO courseDTO) {
        Course course = new Course();
        course.setCode(courseDTO.getCode());
        course.setTitle(courseDTO.getTitle());
        course.setDescription(courseDTO.getDescription());
        
        Course savedCourse = courseRepository.save(course);
        return new CourseDTO(savedCourse.getId(), savedCourse.getCode(), savedCourse.getTitle(), savedCourse.getDescription());
    }

    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + id));
        
        course.setCode(courseDTO.getCode());
        course.setTitle(courseDTO.getTitle());
        course.setDescription(courseDTO.getDescription());
        
        Course updatedCourse = courseRepository.save(course);
        return new CourseDTO(updatedCourse.getId(), updatedCourse.getCode(), updatedCourse.getTitle(), updatedCourse.getDescription());
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}

