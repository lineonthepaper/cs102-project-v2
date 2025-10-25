package com.smartattendance.service;

import com.smartattendance.dto.request.user.AddInstructorRequest;
import com.smartattendance.dto.request.user.UpdateInstructorAssignmentsRequest;
import com.smartattendance.dto.response.attendance.*;
import com.smartattendance.dto.response.course.*;
import com.smartattendance.dto.response.user.*;
import com.smartattendance.dto.response.auth.*;
import com.smartattendance.entity.*;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.*;
import com.smartattendance.util.helper.ServiceUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing instructors.
 * 
 * FIXED: Using ServiceUtils to eliminate code duplication (DRY principle)
 * FIXED: Using ResourceNotFoundException instead of generic RuntimeException
 */
@Service
public class InstructorService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final SectionAssignmentRepository sectionAssignmentRepository;
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final EntityMapper mapper;

    public InstructorService(UserRepository userRepository, SectionAssignmentRepository sectionAssignmentRepository,
                            SectionRepository sectionRepository, CourseRepository courseRepository, EntityMapper mapper) {
        this.userRepository = userRepository;
        this.sectionAssignmentRepository = sectionAssignmentRepository;
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<InstructorDTO> getAllInstructors() {
        List<User> instructors = userRepository.findByIsInstructorTrue();
        
        // FIXED: Using ServiceUtils to eliminate ~20 lines of duplicate code
        Map<String, List<SectionAssignment>> assignmentsByInstructor = ServiceUtils.fetchAndGroupRelated(
            instructors,
            User::getId,  // Extract instructor IDs
            ids -> ids.stream()  // Fetch assignments for these IDs
                .flatMap(id -> sectionAssignmentRepository.findByUserIdAndRoleAndIsActive(id, "INSTRUCTOR", true).stream())
                .collect(Collectors.toList()),
            SectionAssignment::getUserId  // Group by user ID
        );
        
        return instructors.stream()
                .map(instructor -> mapToInstructorDTO(instructor, 
                    assignmentsByInstructor.getOrDefault(instructor.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Transactional
    public InstructorDTO addInstructor(AddInstructorRequest request) {
        try {
            // Check if instructor already exists
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
    
            if (existingUser.isPresent()) {
                // Update existing user to be an instructor
                User user = existingUser.get();
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setIsInstructor(true);
                user.setEnabled(true);
                userRepository.save(user);
                return mapToInstructorDTO(user, Collections.emptyList());
            } else {
                // Create new instructor using native SQL to allow database trigger to generate ID
                String sql = "INSERT INTO users (email, first_name, last_name, is_instructor, is_student, is_ta, enabled, created_at) " +
                             "VALUES (:email, :firstName, :lastName, true, false, false, true, CURRENT_TIMESTAMP) " +
                             "RETURNING id";
                
                String generatedId = (String) entityManager.createNativeQuery(sql)
                        .setParameter("email", request.getEmail())
                        .setParameter("firstName", request.getFirstName())
                        .setParameter("lastName", request.getLastName())
                        .getSingleResult();
    
                // Fetch the newly created instructor
                User savedInstructor = userRepository.findById(generatedId)
                        .orElseThrow(() -> new ResourceNotFoundException("Instructor", generatedId));
    
                return mapToInstructorDTO(savedInstructor, Collections.emptyList());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidRequestException("Instructor could not be added.");
        }
    }

    @Transactional
    public void removeInstructor(String userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor", userId));
    
            // Delete all section assignments
            sectionAssignmentRepository.deleteByUserIdAndRole(userId, "INSTRUCTOR");
    
            // Delete the user completely
            userRepository.delete(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidRequestException("Instructor could not be removed.");
        }
    }

    @Transactional
    public void updateInstructorAssignments(String userId, UpdateInstructorAssignmentsRequest request) {
        try {
            // Verify instructor exists
            User instructor = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor", userId));
    
            // Delete existing instructor assignments
            sectionAssignmentRepository.deleteByUserIdAndRole(userId, "INSTRUCTOR");
    
            // Insert new assignments
            if (request.getSectionIds() != null && !request.getSectionIds().isEmpty()) {
                List<SectionAssignment> newAssignments = request.getSectionIds().stream()
                        .<SectionAssignment>map(sectionId -> new SectionAssignment(userId, sectionId, "INSTRUCTOR", true))
                        .collect(Collectors.toList());
                sectionAssignmentRepository.saveAll(newAssignments);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidRequestException("Instructor assignments could not be updated");
        }
    }

    /**
     * Map User and assignments to InstructorDTO.
     * 
     * REFACTORED: Now uses EntityMapper (DRY principle).
     * Before: Manual DTO construction with nested Course/Section mapping (30+ lines)
     * After: Delegates to EntityMapper for assignment DTOs (2 lines)
     * 
     * This eliminates duplicate mapping logic and ensures consistency.
     */
    private InstructorDTO mapToInstructorDTO(User user, List<SectionAssignment> assignments) {
        // Use EntityMapper to convert assignments (handles Section and Course DTOs)
        List<SectionAssignmentDTO> assignmentDTOs = mapper.toSectionAssignmentDTOs(assignments);

        return new InstructorDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getEnabled(),
                user.getAuthId(),
                assignmentDTOs
        );
    }
}

