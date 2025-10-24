package com.smartattendance.service;

import com.smartattendance.dto.request.CreateStudentRequest;
import com.smartattendance.dto.request.UpdateEnrollmentRequest;
import com.smartattendance.dto.response.*;
import com.smartattendance.entity.*;
import com.smartattendance.exception.DuplicateEmailException;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.exception.UserNotFoundException;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final EntityMapper mapper;

    public StudentService(
            UserRepository userRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            SectionEnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            SectionRepository sectionRepository,
            EntityMapper mapper) {
        this.userRepository = userRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        logger.debug("Fetching all students from database");
        
        // Fetch all students
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getIsStudent())
                .collect(Collectors.toList());
        
        logger.debug("Found {} students in database", students.size());

        // Collect all student IDs
        List<String> studentIds = students.stream()
                .<String>map(u -> u.getId())
                .collect(Collectors.toList());

        // Fetch attendance records for all students in one query
        final Map<String, List<AttendanceRecord>> attendanceByStudent;
        if (!studentIds.isEmpty()) {
            List<AttendanceRecord> allRecords = attendanceRecordRepository.findByUserIdIn(studentIds);
            attendanceByStudent = allRecords.stream()
                    .collect(Collectors.groupingBy(r -> r.getUserId()));
        } else {
            attendanceByStudent = new HashMap<>();
        }

        // Fetch enrollments for all students
        final Map<String, List<SectionEnrollment>> enrollmentsByStudent = new HashMap<>();
        for (String studentId : studentIds) {
            List<SectionEnrollment> enrollments = enrollmentRepository
                    .findByUserIdAndIsActive(studentId, true);
            enrollmentsByStudent.put(studentId, enrollments);
        }

        // Convert to DTOs
        return students.stream()
                .<StudentDTO>map(student -> mapToStudentDTO(
                        student,
                        attendanceByStudent.getOrDefault(student.getId(), Collections.emptyList()),
                        enrollmentsByStudent.getOrDefault(student.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentDTO createStudent(CreateStudentRequest request) {
        logger.info("Creating new student with email: {}", request.getEmail());
        
        // Check if student already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Attempted to create student with duplicate email: {}", request.getEmail());
            throw new DuplicateEmailException("A student with email " + request.getEmail() + " already exists");
        }

        try {
            // Use native SQL to insert student so database trigger can generate the ID
            String sql = "INSERT INTO users (email, first_name, last_name, is_student, is_instructor, is_ta, enabled, created_at) " +
                         "VALUES (:email, :firstName, :lastName, true, false, false, true, CURRENT_TIMESTAMP) " +
                         "RETURNING id";
            
            String generatedId = (String) entityManager.createNativeQuery(sql)
                    .setParameter("email", request.getEmail())
                    .setParameter("firstName", request.getFirstName())
                    .setParameter("lastName", request.getLastName())
                    .getSingleResult();
            
            logger.debug("Student created with ID: {}", generatedId);

            // Fetch the newly created student
            User savedStudent = userRepository.findById(generatedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", generatedId));

            logger.info("Successfully created student: {} {} ({})", request.getFirstName(), request.getLastName(), generatedId);
            return mapToStudentDTO(savedStudent, Collections.emptyList(), Collections.emptyList());
        } catch (Exception e) {
            logger.error("Failed to create student: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to create student", e);
        }
    }

    @Transactional
    public void updateEnrollments(String studentId, UpdateEnrollmentRequest request) {
        logger.info("Updating enrollments for student: {}", studentId);
        
        // Verify student exists
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException("Student not found with ID: " + studentId));

        Set<Long> newSectionIds = new HashSet<>(request.getSectionIds());
        logger.debug("New section IDs for student {}: {}", studentId, newSectionIds);
        
        // Get all existing enrollments (both active and inactive)
        List<SectionEnrollment> existingEnrollments = enrollmentRepository
                .findAll().stream()
                .filter(e -> e.getUserId().equals(studentId))
                .collect(Collectors.toList());

        // Deactivate enrollments not in the new selection
        for (SectionEnrollment enrollment : existingEnrollments) {
            if (!newSectionIds.contains(enrollment.getSectionId())) {
                enrollment.setIsActive(false);
                enrollmentRepository.save(enrollment);
            }
        }

        // Process new selections
        for (Long sectionId : newSectionIds) {
            Optional<SectionEnrollment> existing = enrollmentRepository
                    .findByUserIdAndSectionId(studentId, sectionId);

            if (existing.isPresent()) {
                // Reactivate if it exists but was inactive
                SectionEnrollment enrollment = existing.get();
                if (!enrollment.getIsActive()) {
                    enrollment.setIsActive(true);
                    enrollmentRepository.save(enrollment);
                }
            } else {
                // Create new enrollment
                SectionEnrollment newEnrollment = new SectionEnrollment();
                newEnrollment.setUserId(studentId);
                newEnrollment.setSectionId(sectionId);
                newEnrollment.setIsActive(true);
                newEnrollment.setEnrolledAt(LocalDateTime.now());
                enrollmentRepository.save(newEnrollment);
            }
        }
    }

    private StudentDTO mapToStudentDTO(User student, List<AttendanceRecord> attendanceRecords, List<SectionEnrollment> enrollments) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setDisplayId(normalizeStudentId(student.getId(), student.getIsStudent()));
        dto.setEmail(student.getEmail());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEnabled(student.getEnabled());

        // Calculate attendance statistics
        int totalSessions = attendanceRecords.size();
        int lateSessions = (int) attendanceRecords.stream()
                .filter(r -> "LATE".equals(r.getStatus()))
                .count();
        int presentSessions = (int) attendanceRecords.stream()
                .filter(r -> "PRESENT".equals(r.getStatus()) || "LATE".equals(r.getStatus()))
                .count();
        
        int attendanceRate = totalSessions > 0 ? Math.round((float) presentSessions / totalSessions * 100) : 0;
        int punctualityRate = totalSessions > 0 ? Math.round((float) (totalSessions - lateSessions) / totalSessions * 100) : 0;

        dto.setTotalSessions(totalSessions);
        dto.setPresentSessions(presentSessions);
        dto.setLateSessions(lateSessions);
        dto.setAttendanceRate(attendanceRate);
        dto.setPunctualityRate(punctualityRate);

        // Map attendance records using EntityMapper
        dto.setAttendanceRecords(mapper.toAttendanceRecordDTOs(attendanceRecords));

        // Map enrollments using EntityMapper
        dto.setEnrollments(mapper.toEnrollmentDTOs(enrollments));

        return dto;
    }

    private String normalizeStudentId(String id, Boolean isStudent) {
        if (id == null) return "";
        
        // Check if already normalized (e.g., S0000001)
        if (id.matches("^[A-Z]\\d{7}$")) {
            return id;
        }
        
        // If it's just digits, add prefix
        if (id.matches("^\\d+$") && isStudent) {
            return "S" + String.format("%07d", Integer.parseInt(id));
        }
        
        return id;
    }
}

