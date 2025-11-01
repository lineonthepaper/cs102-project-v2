package com.smartattendance.service;

import com.smartattendance.dto.request.user.CreateStudentRequest;
import com.smartattendance.dto.request.user.UpdateEnrollmentRequest;
import com.smartattendance.dto.request.user.UpdateStudentRequest;
import com.smartattendance.dto.response.user.*;
import com.smartattendance.entity.*;
import com.smartattendance.exception.DuplicateEmailException;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.*;
import com.smartattendance.util.converter.ImageConverter;
import java.time.OffsetDateTime;
import com.smartattendance.util.helper.ServiceUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final EntityMapper mapper;
    private final ObjectMapper objectMapper;
    private final FaceRecognitionService faceRecognitionService;

    public StudentService(
            UserRepository userRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            SectionEnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            EntityMapper mapper,
            ObjectMapper objectMapper,
            FaceRecognitionService faceRecognitionService) {
        this.userRepository = userRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.faceRecognitionService = faceRecognitionService;
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        logger.debug("Fetching all students from database");
        
        // Fetch all students
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getIsStudent())
                .collect(Collectors.toList());
        
        logger.debug("Found {} students in database", students.size());

        // Fetch attendance records for all students        
        Map<String, List<AttendanceRecord>> attendanceByStudent = ServiceUtils.fetchAndGroupRelated(
            students,
            User::getId,
            ids -> !ids.isEmpty() ? attendanceRecordRepository.findByUserIdIn(ids) : Collections.emptyList(),
            AttendanceRecord::getUserId
        );

        // Fetch enrollments for all students in a single batch query (fixes N+1 problem)
        Map<String, List<SectionEnrollment>> enrollmentsByStudent = ServiceUtils.fetchAndGroupRelated(
            students,
            User::getId,
            ids -> !ids.isEmpty() ? enrollmentRepository.findByUserIdInAndIsActive(ids, true) : Collections.emptyList(),
            SectionEnrollment::getUserId
        );

        // Convert to DTOs
        return students.stream()
                .<StudentDTO>map(student -> mapToStudentDTO(
                        student,
                        attendanceByStudent.getOrDefault(student.getId(), Collections.emptyList()),
                        enrollmentsByStudent.getOrDefault(student.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentDTO getStudentById(String studentId) {
        logger.debug("Fetching student by ID: {}", studentId);
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        
        // Verify it's actually a student
        if (!student.getIsStudent()) {
            throw new ResourceNotFoundException("Student", studentId);
        }
        
        // Fetch attendance records for this student
        List<AttendanceRecord> attendanceRecords = attendanceRecordRepository.findByUserIdWithDetails(studentId);
        
        // Fetch enrollments for this student
        List<SectionEnrollment> enrollments = enrollmentRepository.findByUserIdAndIsActive(studentId, true);
        
        // Convert to DTO
        return mapToStudentDTO(student, attendanceRecords, enrollments);
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
            List<String> faceImages = Optional.ofNullable(request.getFaceImages())
                    .map(images -> images.stream()
                            .map(ImageConverter::base64ToBytes)
                            .map(ImageConverter::bytesToBase64)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());

            if (faceImages.size() > 8) {
                throw new IllegalArgumentException("Face images cannot exceed 8 entries");
            }

            String faceImagesJson = serializeFaceImages(faceImages);

            // Build face profiles (embeddings) from provided images
            List<FaceProfile> profiles = new ArrayList<>();
            for (String b64 : faceImages) {
                byte[] bytes = ImageConverter.base64ToBytes(b64);
                faceRecognitionService.computeEmbedding(bytes).ifPresent(emb -> {
                    profiles.add(new FaceProfile(emb, OffsetDateTime.now()));
                });
            }

            String faceProfilesJson = objectMapper.writeValueAsString(profiles);

            // Use native SQL to insert student so database trigger can generate the ID
            String sql = "INSERT INTO users (email, first_name, last_name, face_images, face_profiles, is_student, is_instructor, is_ta, enabled, created_at) " +
                         "VALUES (:email, :firstName, :lastName, CAST(:faceImages AS jsonb), CAST(:faceProfiles AS jsonb), true, false, false, true, CURRENT_TIMESTAMP) " +
                         "RETURNING id";
            
            String generatedId = (String) entityManager.createNativeQuery(sql)
                    .setParameter("email", request.getEmail())
                    .setParameter("firstName", request.getFirstName())
                    .setParameter("lastName", request.getLastName())
                    .setParameter("faceImages", faceImagesJson)
                    .setParameter("faceProfiles", faceProfilesJson)
                    .getSingleResult();
            
            logger.debug("Student created with ID: {}", generatedId);

            // Fetch the newly created student
            User savedStudent = userRepository.findById(generatedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", generatedId));

            StudentDTO dto = mapToStudentDTO(savedStudent, Collections.emptyList(), Collections.emptyList());
            dto.setFaceImages(new ArrayList<>(faceImages));

            logger.info("Successfully created student: {} {} ({})", request.getFirstName(), request.getLastName(), generatedId);
            return dto;
        } catch (Exception e) {
            logger.error("Failed to create student: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to create student", e);
        }
    }

    @Transactional
    public void updateEnrollments(String studentId, UpdateEnrollmentRequest request) {
        logger.info("Updating enrollments for student: {}", studentId);
        
        // Verify student exists
        userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

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

    @Transactional
    public StudentDTO updateStudent(String studentId, UpdateStudentRequest request) {
        logger.info("Updating student: {}", studentId);
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        
        // Check for email duplicate if email is changing
        if (!student.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                logger.warn("Attempted to update student with duplicate email: {}", request.getEmail());
                throw new DuplicateEmailException("A student with email " + request.getEmail() + " already exists");
            }
        }
        
        // Update basic fields
        student.setEmail(request.getEmail());
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        
        // Update face images if provided
        if (request.getFaceImages() != null && !request.getFaceImages().isEmpty()) {
            try {
                List<String> faceImages = request.getFaceImages().stream()
                        .map(ImageConverter::base64ToBytes)
                        .map(ImageConverter::bytesToBase64)
                        .collect(Collectors.toList());

                if (faceImages.size() > 8) {
                    throw new IllegalArgumentException("Face images cannot exceed 8 entries");
                }

                String faceImagesJson = serializeFaceImages(faceImages);

                // Build face profiles (embeddings) from provided images
                List<FaceProfile> profiles = new ArrayList<>();
                for (String b64 : faceImages) {
                    byte[] bytes = ImageConverter.base64ToBytes(b64);
                    faceRecognitionService.computeEmbedding(bytes).ifPresent(emb -> {
                        profiles.add(new FaceProfile(emb, OffsetDateTime.now()));
                    });
                }

                String faceProfilesJson = objectMapper.writeValueAsString(profiles);

                // Use native SQL to update face images and profiles
                String sql = "UPDATE users SET face_images = CAST(:faceImages AS jsonb), face_profiles = CAST(:faceProfiles AS jsonb) WHERE id = :id";
                entityManager.createNativeQuery(sql)
                        .setParameter("faceImages", faceImagesJson)
                        .setParameter("faceProfiles", faceProfilesJson)
                        .setParameter("id", studentId)
                        .executeUpdate();
                
                logger.info("Face images and profiles updated for student: {}", studentId);
            } catch (Exception e) {
                logger.error("Failed to update face images for student: {}", studentId, e);
                throw new RuntimeException("Failed to update face images", e);
            }
        }
        
        User updatedStudent = userRepository.save(student);
        logger.info("Student updated successfully: {}", studentId);
        
        // Return updated student without full attendance records for performance
        return mapToStudentSummaryDTO(updatedStudent, Collections.emptyList());
    }

    @Transactional
    public void deleteStudent(String studentId) {
        logger.info("Deleting student: {}", studentId);
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        
        userRepository.delete(student);
        logger.info("Student deleted successfully: {}", studentId);
    }

    /**
     * Get students summary for list views - lightweight and fast.
     * Optionally filter by course or section.
     * Does NOT include individual attendance records in response.
     * 
     * @param courseId optional course filter
     * @param sectionId optional section filter
     * @return list of students with calculated stats but no attendance records
     */
    @Transactional(readOnly = true)
    public List<StudentDTO> getStudentsSummary(Long courseId, Long sectionId) {
        logger.debug("Fetching students summary - courseId: {}, sectionId: {}", courseId, sectionId);
        
        // Fetch all students
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getIsStudent())
                .collect(Collectors.toList());
        
        // If filtering by section or course, filter students by enrollment
        if (sectionId != null || courseId != null) {
            // Get enrollments
            Map<String, List<SectionEnrollment>> enrollmentsByStudent = ServiceUtils.fetchAndGroupRelated(
                students,
                User::getId,
                ids -> !ids.isEmpty() ? enrollmentRepository.findByUserIdInAndIsActive(ids, true) : Collections.emptyList(),
                SectionEnrollment::getUserId
            );
            
            // If section filter is specified, we need to check sections
            if (sectionId != null) {
                students = students.stream()
                    .filter(student -> {
                        List<SectionEnrollment> enrollments = enrollmentsByStudent.getOrDefault(student.getId(), Collections.emptyList());
                        return enrollments.stream().anyMatch(e -> e.getSectionId().equals(sectionId));
                    })
                    .collect(Collectors.toList());
            } else if (courseId != null) {
                // Filter by course - need to fetch sections to check course_id
                List<Section> allSections = sectionRepository.findByCourseId(courseId);
                Set<Long> sectionIdsInCourse = allSections.stream()
                    .map(Section::getId)
                    .collect(Collectors.toSet());
                
                students = students.stream()
                    .filter(student -> {
                        List<SectionEnrollment> enrollments = enrollmentsByStudent.getOrDefault(student.getId(), Collections.emptyList());
                        return enrollments.stream().anyMatch(e -> sectionIdsInCourse.contains(e.getSectionId()));
                    })
                    .collect(Collectors.toList());
            }
        }
        
        // Fetch attendance records for filtered students
        Map<String, List<AttendanceRecord>> attendanceByStudent = ServiceUtils.fetchAndGroupRelated(
            students,
            User::getId,
            ids -> !ids.isEmpty() ? attendanceRecordRepository.findByUserIdIn(ids) : Collections.emptyList(),
            AttendanceRecord::getUserId
        );
        
        // Map to lightweight DTOs (stats only, no attendance records)
        return students.stream()
                .map(student -> {
                    List<AttendanceRecord> records = attendanceByStudent.getOrDefault(student.getId(), Collections.emptyList());
                    
                    // If filtering by course/section, filter attendance records too
                    if (sectionId != null || courseId != null) {
                        records = filterAttendanceRecordsByCourseOrSection(records, courseId, sectionId);
                    }
                    
                    return mapToStudentSummaryDTO(student, records);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Filter attendance records by course or section using batch queries.
     */
    private List<AttendanceRecord> filterAttendanceRecordsByCourseOrSection(
            List<AttendanceRecord> records, Long courseId, Long sectionId) {
        
        if (records.isEmpty()) {
            return records;
        }
        
        // Batch fetch all sessions for the records
        Set<Long> sessionIds = records.stream()
            .map(AttendanceRecord::getSessionId)
            .collect(Collectors.toSet());
        
        List<AttendanceSession> sessions = attendanceSessionRepository.findAllById(sessionIds);
        Map<Long, Long> sessionToSectionMap = sessions.stream()
            .collect(Collectors.toMap(
                AttendanceSession::getId,
                AttendanceSession::getSectionId
            ));
        
        if (sectionId != null) {
            // Filter by section
            return records.stream()
                .filter(record -> {
                    Long sessionSectionId = sessionToSectionMap.get(record.getSessionId());
                    return sectionId.equals(sessionSectionId);
                })
                .collect(Collectors.toList());
        } else if (courseId != null) {
            // Filter by course - get all section IDs in the course
            List<Section> sectionsInCourse = sectionRepository.findByCourseId(courseId);
            Set<Long> sectionIdsInCourse = sectionsInCourse.stream()
                .map(Section::getId)
                .collect(Collectors.toSet());
            
            return records.stream()
                .filter(record -> {
                    Long sessionSectionId = sessionToSectionMap.get(record.getSessionId());
                    return sessionSectionId != null && sectionIdsInCourse.contains(sessionSectionId);
                })
                .collect(Collectors.toList());
        }
        
        return records;
    }
    
    /**
     * Map to lightweight summary DTO without attendance records.
     */
    private StudentDTO mapToStudentSummaryDTO(User student, List<AttendanceRecord> attendanceRecords) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setDisplayId(student.getNormalizedStudentId());
        dto.setEmail(student.getEmail());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEnabled(student.getEnabled());
        dto.setFaceImages(student.getFaceImages() != null ? new ArrayList<>(student.getFaceImages()) : Collections.emptyList());

        // Calculate stats but don't include individual records
        User.AttendanceStatistics stats = student.calculateAttendanceStats(attendanceRecords);
        dto.setTotalSessions(stats.totalSessions);
        dto.setPresentSessions(stats.presentSessions);
        dto.setLateSessions(stats.lateSessions);
        dto.setAttendanceRate(stats.attendanceRate);
        dto.setPunctualityRate(stats.punctualityRate);

        // NO attendance records or enrollments for performance
        dto.setAttendanceRecords(null);
        dto.setEnrollments(null);

        return dto;
    }

    /**
     * Map a User entity to StudentDTO.
     * Now using Rich Domain Model - the entity does the work!
     */
    private StudentDTO mapToStudentDTO(User student, List<AttendanceRecord> attendanceRecords, List<SectionEnrollment> enrollments) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setDisplayId(student.getNormalizedStudentId()); // Tell, don't ask!
        dto.setEmail(student.getEmail());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEnabled(student.getEnabled());
        dto.setFaceImages(student.getFaceImages() != null ? new ArrayList<>(student.getFaceImages()) : Collections.emptyList());

        // Let the entity calculate its own statistics (Rich Domain Model)
        User.AttendanceStatistics stats = student.calculateAttendanceStats(attendanceRecords);
        dto.setTotalSessions(stats.totalSessions);
        dto.setPresentSessions(stats.presentSessions);
        dto.setLateSessions(stats.lateSessions);
        dto.setAttendanceRate(stats.attendanceRate);
        dto.setPunctualityRate(stats.punctualityRate);

        // Map relationships using EntityMapper
        dto.setAttendanceRecords(mapper.toAttendanceRecordDTOs(attendanceRecords));
        dto.setEnrollments(mapper.toEnrollmentDTOs(enrollments));

        return dto;
    }

    private String serializeFaceImages(List<String> faceImages) {
        try {
            return objectMapper.writeValueAsString(faceImages);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize face images", e);
        }
    }
}

