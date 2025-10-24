package com.smartattendance.service;

import com.smartattendance.dto.request.user.AddTARequest;
import com.smartattendance.dto.request.user.UpdateTAAssignmentsRequest;
import com.smartattendance.dto.response.attendance.*;
import com.smartattendance.dto.response.course.*;
import com.smartattendance.dto.response.user.*;
import com.smartattendance.dto.response.auth.*;
import com.smartattendance.entity.*;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.exception.InvalidRequestException;
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
 * Service for managing TAs.
 * 
 * FIXED: Using ServiceUtils to eliminate code duplication (DRY principle)
 * FIXED: Using ResourceNotFoundException instead of generic RuntimeException
 */
@Service
public class TAService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final TAAssignmentRepository taAssignmentRepository;
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final EntityMapper mapper;

    public TAService(UserRepository userRepository, TAAssignmentRepository taAssignmentRepository,
                    SectionRepository sectionRepository, CourseRepository courseRepository, EntityMapper mapper) {
        this.userRepository = userRepository;
        this.taAssignmentRepository = taAssignmentRepository;
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TADTO> getAllTAs() {
        List<User> tas = userRepository.findByIsTATrue();
        
        // FIXED: Using ServiceUtils to eliminate ~20 lines of duplicate code
        Map<String, List<TAAssignment>> assignmentsByTA = ServiceUtils.fetchAndGroupRelated(
            tas,
            User::getId,  // Extract TA IDs
            ids -> ids.stream()  // Fetch assignments for these IDs
                .flatMap(id -> taAssignmentRepository.findByUserId(id).stream())
                .collect(Collectors.toList()),
            TAAssignment::getUserId  // Group by user ID
        );
        
        return tas.stream()
                .map(ta -> mapToTADTO(ta, 
                    assignmentsByTA.getOrDefault(ta.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Transactional
    public TADTO addTA(AddTARequest request) {
        // Find existing user by email
        String resourceType = request.getType().equals("instructor") ? "Instructor" : "Student";
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(resourceType, request.getEmail()));

        // Check if already a TA
        if (Boolean.TRUE.equals(existingUser.getIsTA())) {
            throw new InvalidRequestException("This user is already a Teaching Assistant");
        }

        // Validate user type
        if (request.getType().equals("instructor") && !Boolean.TRUE.equals(existingUser.getIsInstructor())) {
            throw new InvalidRequestException("This user is not an instructor. Please check the email or select Student.");
        }
        if (request.getType().equals("student") && !Boolean.TRUE.equals(existingUser.getIsStudent())) {
            throw new InvalidRequestException("This user is not a student. Please check the email or select Instructor.");
        }

        // Note: Frontend will handle Supabase Auth creation for students
        // Backend just updates the database record

        // Update user to be a TA
        existingUser.setIsTA(true);
        existingUser.setEnabled(true);
        userRepository.save(existingUser);

        return mapToTADTO(existingUser, Collections.emptyList());
    }

    @Transactional
    public void removeTA(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("TA", userId));

        // Delete all TA assignments
        taAssignmentRepository.deleteByUserId(userId);

        // Update user to remove TA status
        user.setIsTA(false);
        user.setEnabled(false);
        user.setAuthId(null);
        userRepository.save(user);
    }

    @Transactional
    public void updateTAAssignments(String userId, UpdateTAAssignmentsRequest request) {
        // Verify TA exists
        User ta = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("TA", userId));

        // Delete existing assignments
        taAssignmentRepository.deleteByUserId(userId);

        // Insert new assignments
        if (request.getSectionIds() != null && !request.getSectionIds().isEmpty()) {
            List<TAAssignment> newAssignments = request.getSectionIds().stream()
                    .<TAAssignment>map(sectionId -> new TAAssignment(userId, sectionId))
                    .collect(Collectors.toList());
            taAssignmentRepository.saveAll(newAssignments);
        }
    }

    /**
     * Map User and assignments to TADTO.
     * 
     * REFACTORED: Now uses EntityMapper (DRY principle).
     * Before: Manual DTO construction with nested Course/Section mapping (30+ lines)
     * After: Delegates to EntityMapper for assignment DTOs (2 lines)
     * 
     * This eliminates duplicate mapping logic and ensures consistency.
     */
    private TADTO mapToTADTO(User user, List<TAAssignment> assignments) {
        // Use EntityMapper to convert assignments (handles Section and Course DTOs)
        List<TAAssignmentDTO> assignmentDTOs = mapper.toTAAssignmentDTOs(assignments);

        return new TADTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getIsStudent(),
                user.getIsInstructor(),
                user.getIsTA(),
                user.getEnabled(),
                user.getAuthId(),
                assignmentDTOs
        );
    }
    
}

