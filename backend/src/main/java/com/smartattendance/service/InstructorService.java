package com.smartattendance.service;

import com.smartattendance.dto.request.AddInstructorRequest;
import com.smartattendance.dto.request.UpdateInstructorAssignmentsRequest;
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
        
        // Fetch all instructor IDs
        List<String> instructorIds = instructors.stream().<String>map(u -> u.getId()).collect(Collectors.toList());
        
        // Fetch all section assignments for these instructors
        final Map<String, List<SectionAssignment>> assignmentsByInstructor;
        if (!instructorIds.isEmpty()) {
            List<SectionAssignment> allAssignments = instructorIds.stream()
                    .flatMap(instructorId -> sectionAssignmentRepository.findByUserIdAndRoleAndIsActive(instructorId, "INSTRUCTOR", true).stream())
                    .collect(Collectors.toList());
            assignmentsByInstructor = allAssignments.stream()
                    .collect(Collectors.groupingBy(a -> a.getUserId()));
        } else {
            assignmentsByInstructor = new HashMap<>();
        }
        
        return instructors.stream()
                .<InstructorDTO>map(instructor -> mapToInstructorDTO(instructor, assignmentsByInstructor.getOrDefault(instructor.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Transactional
    public InstructorDTO addInstructor(AddInstructorRequest request) {
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
                    .orElseThrow(() -> new RuntimeException("Failed to fetch created instructor"));

            return mapToInstructorDTO(savedInstructor, Collections.emptyList());
        }
    }

    @Transactional
    public void removeInstructor(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Instructor not found with ID: " + userId));

        // Delete all section assignments
        sectionAssignmentRepository.deleteByUserIdAndRole(userId, "INSTRUCTOR");

        // Delete the user completely
        userRepository.delete(user);
    }

    @Transactional
    public void updateInstructorAssignments(String userId, UpdateInstructorAssignmentsRequest request) {
        // Verify instructor exists
        User instructor = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Instructor not found with ID: " + userId));

        // Delete existing instructor assignments
        sectionAssignmentRepository.deleteByUserIdAndRole(userId, "INSTRUCTOR");

        // Insert new assignments
        if (request.getSectionIds() != null && !request.getSectionIds().isEmpty()) {
            List<SectionAssignment> newAssignments = request.getSectionIds().stream()
                    .<SectionAssignment>map(sectionId -> new SectionAssignment(userId, sectionId, "INSTRUCTOR", true))
                    .collect(Collectors.toList());
            sectionAssignmentRepository.saveAll(newAssignments);
        }
    }

    private InstructorDTO mapToInstructorDTO(User user, List<SectionAssignment> assignments) {
        List<SectionAssignmentDTO> assignmentDTOs = assignments.stream()
                .<SectionAssignmentDTO>map(assignment -> {
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
                        sectionDTO =                         new SectionDTO(
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
                    return new SectionAssignmentDTO(assignment.getId(), assignment.getUserId(), assignment.getSectionId(), 
                                                    assignment.getRole(), assignment.getIsActive(), sectionDTO);
                })
                .collect(Collectors.toList());

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

