package com.smartattendance.service;

import com.smartattendance.dto.request.AddTARequest;
import com.smartattendance.dto.request.UpdateTAAssignmentsRequest;
import com.smartattendance.dto.response.*;
import com.smartattendance.entity.*;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.*;
import com.smartattendance.util.DateTimeUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
        
        // Fetch all TA IDs
        List<String> taIds = tas.stream().<String>map(u -> u.getId()).collect(Collectors.toList());
        
        // Fetch all TA assignments for these TAs
        final Map<String, List<TAAssignment>> assignmentsByTA;
        if (!taIds.isEmpty()) {
            List<TAAssignment> allAssignments = taIds.stream()
                    .flatMap(taId -> taAssignmentRepository.findByUserId(taId).stream())
                    .collect(Collectors.toList());
            assignmentsByTA = allAssignments.stream()
                    .collect(Collectors.groupingBy(a -> a.getUserId()));
        } else {
            assignmentsByTA = new HashMap<>();
        }
        
        return tas.stream()
                .<TADTO>map(ta -> mapToTADTO(ta, assignmentsByTA.getOrDefault(ta.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Transactional
    public TADTO addTA(AddTARequest request) {
        // Find existing user by email
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        (request.getType().equals("instructor") ? "Instructor" : "Student") + 
                        " with this email not found"));

        // Check if already a TA
        if (Boolean.TRUE.equals(existingUser.getIsTA())) {
            throw new RuntimeException("This user is already a Teaching Assistant");
        }

        // Validate user type
        if (request.getType().equals("instructor") && !Boolean.TRUE.equals(existingUser.getIsInstructor())) {
            throw new RuntimeException("This user is not an instructor. Please check the email or select Student.");
        }
        if (request.getType().equals("student") && !Boolean.TRUE.equals(existingUser.getIsStudent())) {
            throw new RuntimeException("This user is not a student. Please check the email or select Instructor.");
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
                .orElseThrow(() -> new RuntimeException("TA not found with ID: " + userId));

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
                .orElseThrow(() -> new RuntimeException("TA not found with ID: " + userId));

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

    private TADTO mapToTADTO(User user, List<TAAssignment> assignments) {
        List<TAAssignmentDTO> assignmentDTOs = assignments.stream()
                .<TAAssignmentDTO>map(assignment -> {
                    SectionDTO sectionDTO = null;
                    if (assignment.getSection() != null) {
                        CourseDTO courseDTO = null;
                        if (assignment.getSection().getCourse() != null) {
                            courseDTO = new CourseDTO(
                                    assignment.getSection().getCourse().getId(),
                                    assignment.getSection().getCourse().getCode(),
                                    assignment.getSection().getCourse().getTitle(),
                                    assignment.getSection().getCourse().getDescription()
                            );
                        }
                        sectionDTO = new SectionDTO(
                                assignment.getSection().getId(),
                                assignment.getSection().getSectionCode(),
                                assignment.getSection().getCourseId(),
                                assignment.getSection().getYear(),
                                assignment.getSection().getSemester(),
                                DateTimeUtils.dayNumberToName(assignment.getSection().getMeetingDay()),
                                assignment.getSection().getStartTime(),
                                assignment.getSection().getEndTime(),
                                assignment.getSection().getLocation(),
                                courseDTO
                        );
                    }
                    return new TAAssignmentDTO(assignment.getId(), assignment.getUserId(), assignment.getSectionId(), sectionDTO);
                })
                .collect(Collectors.toList());

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

