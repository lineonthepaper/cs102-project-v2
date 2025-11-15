package com.smartattendance.service;

import com.smartattendance.dto.request.user.CreateStudentRequest;
import com.smartattendance.dto.request.user.UpdateEnrollmentRequest;
import com.smartattendance.dto.request.user.UpdateStudentRequest;
import com.smartattendance.dto.response.face.FaceImageProcessingResultDTO;
import com.smartattendance.dto.response.face.FaceProcessingSummaryDTO;
import com.smartattendance.dto.response.user.*;
import com.smartattendance.entity.*;
import com.smartattendance.exception.DuplicateEmailException;
import com.smartattendance.exception.ResourceNotFoundException;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.*;
import com.smartattendance.util.converter.ImageConverter;
import com.smartattendance.util.opencv.FaceExtractionError;
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
    private static final String STUDENT_ID_PREFIX = "S";
    private static final int ID_NUMBER_LENGTH = 7;

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

    private SessionRecognitionManager sessionRecognitionManager;

    public StudentService(
            UserRepository userRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            SectionEnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            EntityMapper mapper,
            ObjectMapper objectMapper,
            FaceRecognitionService faceRecognitionService,
            SessionRecognitionManager sessionRecognitionManager) {
        this.userRepository = userRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.faceRecognitionService = faceRecognitionService;
        this.sessionRecognitionManager = sessionRecognitionManager;
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
                AttendanceRecord::getUserId);

        // Fetch enrollments for all students in a single batch query (fixes N+1
        // problem)
        Map<String, List<SectionEnrollment>> enrollmentsByStudent = ServiceUtils.fetchAndGroupRelated(
                students,
                User::getId,
                ids -> !ids.isEmpty() ? enrollmentRepository.findByUserIdInAndIsActive(ids, true)
                        : Collections.emptyList(),
                SectionEnrollment::getUserId);

        // Convert to DTOs
        return students.stream()
                .<StudentDTO>map(student -> mapToStudentDTO(
                        student,
                        attendanceByStudent.getOrDefault(student.getId(), Collections.emptyList()),
                        enrollmentsByStudent.getOrDefault(student.getId(), Collections.emptyList())))
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
    public StudentFaceProcessingResult createStudent(CreateStudentRequest request) {
        logger.info("createStudent called - Name: {} {}, Email received: {}",
                request.getFirstName(), request.getLastName(), request.getEmail());

        logger.info("Creating new student with email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Attempted to create student with duplicate email: {}", request.getEmail());
            throw new DuplicateEmailException("A student with email " + request.getEmail() + " already exists");
        }

        try {
            String newStudentId = generateNextStudentId();

            List<String> incomingFaceImages = Optional.ofNullable(request.getFaceImages())
                    .orElse(Collections.emptyList());

            if (incomingFaceImages.size() > 8) {
                throw new IllegalArgumentException("Face images cannot exceed 8 entries");
            }

            List<FaceImageProcessingResultDTO> processingResults = new ArrayList<>();
            List<String> acceptedFaceImages = new ArrayList<>();
            List<FaceProfile> profiles = new ArrayList<>();

            int index = 0;
            for (String faceImage : incomingFaceImages) {
                if (faceImage == null || faceImage.isBlank()) {
                    processingResults.add(FaceImageProcessingResultDTO.rejected(
                            index,
                            FaceExtractionError.IMAGE_DECODE_FAILED,
                            "Image data is empty."));
                    index++;
                    continue;
                }

                byte[] imageBytes;
                try {
                    imageBytes = ImageConverter.base64ToBytes(faceImage);
                } catch (IllegalArgumentException ex) {
                    logger.warn("Invalid base64 image data for student {} {} (index {})",
                            request.getFirstName(), request.getLastName(), index);
                    processingResults.add(FaceImageProcessingResultDTO.rejected(
                            index,
                            FaceExtractionError.IMAGE_DECODE_FAILED,
                            "Invalid base64 image data."));
                    index++;
                    continue;
                }

                FaceRecognitionService.FaceEmbeddingResult embeddingResult = faceRecognitionService
                        .computeEmbeddingDetailed(imageBytes);

                if (embeddingResult.isSuccess()) {
                    String normalized = ImageConverter.bytesToBase64(imageBytes);
                    acceptedFaceImages.add(normalized);
                    profiles.add(new FaceProfile(embeddingResult.getEmbedding(), OffsetDateTime.now()));
                    processingResults.add(FaceImageProcessingResultDTO.accepted(
                            index,
                            "Face image accepted."));
                } else {
                    FaceExtractionError errorCode = embeddingResult.getExtractionOutcome() != null
                            ? embeddingResult.getExtractionOutcome().getError()
                            : null;
                    String message = embeddingResult.getMessage() != null
                            ? embeddingResult.getMessage()
                            : "Face extraction failed.";
                    processingResults.add(FaceImageProcessingResultDTO.rejected(
                            index,
                            errorCode,
                            message));
                }

                index++;
            }

            FaceProcessingSummaryDTO faceProcessingSummary = FaceProcessingSummaryDTO.from(
                    incomingFaceImages.size(),
                    acceptedFaceImages.size(),
                    processingResults);

            if (faceProcessingSummary.hasFailures()) {
                logger.warn("Face processing completed with {} rejected image(s) for student {} {}",
                        faceProcessingSummary.getRejectedCount(),
                        request.getFirstName(),
                        request.getLastName());
            }

            if (!incomingFaceImages.isEmpty() && acceptedFaceImages.isEmpty()) {
                throw new InvalidRequestException(
                        "All provided face images were rejected. Please upload clearer photos.");
            }

            String faceImagesJson = serializeFaceImages(acceptedFaceImages);
            String faceProfilesJson = objectMapper.writeValueAsString(profiles);

            String sql = "INSERT INTO users (id, email, first_name, last_name, face_images, face_profiles, is_student, is_instructor, is_ta, enabled, created_at) "
                    +
                    "VALUES (:id, :email, :firstName, :lastName, CAST(:faceImages AS jsonb), CAST(:faceProfiles AS jsonb), true, false, false, true, CURRENT_TIMESTAMP) "
                    +
                    "RETURNING id";

            String generatedId = (String) entityManager.createNativeQuery(sql)
                    .setParameter("id", newStudentId)
                    .setParameter("email", request.getEmail())
                    .setParameter("firstName", request.getFirstName())
                    .setParameter("lastName", request.getLastName())
                    .setParameter("faceImages", faceImagesJson)
                    .setParameter("faceProfiles", faceProfilesJson)
                    .getSingleResult();

            logger.debug("Student created with ID: {}", generatedId);

            User savedStudent = userRepository.findById(generatedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", generatedId));

            StudentDTO dto = mapToStudentDTO(savedStudent, Collections.emptyList(), Collections.emptyList());
            dto.setFaceImages(new ArrayList<>(acceptedFaceImages));

            logger.info("Successfully created student: {} {} ({})", request.getFirstName(), request.getLastName(),
                    generatedId);

            return new StudentFaceProcessingResult(dto, faceProcessingSummary);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create student: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to create student", e);
        }
    }

    private String generateNextStudentId() {
        int nextNumber = 1;

        Optional<User> latestStudent = userRepository.findFirstByIdStartingWithOrderByIdDesc(STUDENT_ID_PREFIX);

        if (latestStudent.isPresent()) {
            String latestId = latestStudent.get().getId();
            if (latestId != null) {
                String numericPart = latestId.startsWith(STUDENT_ID_PREFIX)
                        ? latestId.substring(STUDENT_ID_PREFIX.length())
                        : latestId;
                if (numericPart.matches("\\d+")) {
                    try {
                        nextNumber = Integer.parseInt(numericPart) + 1;
                    } catch (NumberFormatException ignored) {
                        nextNumber = 1;
                    }
                }
            }
        }

        if (nextNumber < 1) {
            nextNumber = 1;
        }

        return STUDENT_ID_PREFIX + String.format("%0" + ID_NUMBER_LENGTH + "d", nextNumber);
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

        sessionRecognitionManager.invalidateSessionsForUser(studentId);
        logger.info("Invalidated session cache after enrollment update for student: {}", studentId);
    }

    @Transactional
    public StudentFaceProcessingResult updateStudent(String studentId, UpdateStudentRequest request) {
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

        FaceProcessingSummaryDTO faceProcessingSummary = null;

        // Update face images if provided (including empty list to clear images)
        // Persist basic field changes before handling face images (native updates
        // bypass JPA tracking)
        entityManager.flush();

        if (request.getFaceImages() != null) {
            logger.info("Updating face images for student {}: {} images", studentId, request.getFaceImages().size());
            try {
                List<String> incomingFaceImages = request.getFaceImages();
                if (incomingFaceImages.size() > 8) {
                    throw new IllegalArgumentException("Face images cannot exceed 8 entries");
                }

                List<FaceImageProcessingResultDTO> processingResults = new ArrayList<>();
                List<String> acceptedFaceImages = new ArrayList<>();
                List<FaceProfile> profiles = new ArrayList<>();

                int index = 0;
                for (String faceImage : incomingFaceImages) {
                    if (faceImage == null || faceImage.isBlank()) {
                        processingResults.add(FaceImageProcessingResultDTO.rejected(
                                index,
                                FaceExtractionError.IMAGE_DECODE_FAILED,
                                "Image data is empty."));
                        index++;
                        continue;
                    }

                    byte[] imageBytes;
                    try {
                        imageBytes = ImageConverter.base64ToBytes(faceImage);
                    } catch (IllegalArgumentException ex) {
                        logger.warn("Invalid base64 image data for student {} (index {})", studentId, index);
                        processingResults.add(FaceImageProcessingResultDTO.rejected(
                                index,
                                FaceExtractionError.IMAGE_DECODE_FAILED,
                                "Invalid base64 image data."));
                        index++;
                        continue;
                    }

                    FaceRecognitionService.FaceEmbeddingResult embeddingResult = faceRecognitionService
                            .computeEmbeddingDetailed(imageBytes);

                    if (embeddingResult.isSuccess()) {
                        String normalized = ImageConverter.bytesToBase64(imageBytes);
                        acceptedFaceImages.add(normalized);
                        profiles.add(new FaceProfile(embeddingResult.getEmbedding(), OffsetDateTime.now()));
                        processingResults.add(FaceImageProcessingResultDTO.accepted(
                                index,
                                "Face image accepted."));
                    } else {
                        FaceExtractionError errorCode = embeddingResult.getExtractionOutcome() != null
                                ? embeddingResult.getExtractionOutcome().getError()
                                : null;
                        String message = embeddingResult.getMessage() != null
                                ? embeddingResult.getMessage()
                                : "Face extraction failed.";
                        processingResults.add(FaceImageProcessingResultDTO.rejected(
                                index,
                                errorCode,
                                message));
                    }

                    index++;
                }

                faceProcessingSummary = FaceProcessingSummaryDTO.from(
                        incomingFaceImages.size(),
                        acceptedFaceImages.size(),
                        processingResults);

                if (faceProcessingSummary.hasFailures()) {
                    logger.warn("Face processing completed with {} rejected image(s) for student {}",
                            faceProcessingSummary.getRejectedCount(), studentId);
                }

                if (!incomingFaceImages.isEmpty() && acceptedFaceImages.isEmpty()) {
                    throw new InvalidRequestException(
                            "All provided face images were rejected. Please upload clearer photos.");
                }

                String faceImagesJson = serializeFaceImages(acceptedFaceImages);
                // logger.info("Serialized face images JSON: {}", faceImagesJson);

                String faceProfilesJson = objectMapper.writeValueAsString(profiles);
                // logger.info("Serialized face profiles JSON: {}", faceProfilesJson);

                String sql = "UPDATE users SET face_images = CAST(:faceImages AS jsonb), face_profiles = CAST(:faceProfiles AS jsonb) WHERE id = :id";
                entityManager.createNativeQuery(sql)
                        .setParameter("faceImages", faceImagesJson)
                        .setParameter("faceProfiles", faceProfilesJson)
                        .setParameter("id", studentId)
                        .executeUpdate();

                logger.info("Face images and profiles updated for student: {} ({} images, {} profiles)",
                        studentId, acceptedFaceImages.size(), profiles.size());
            } catch (InvalidRequestException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Failed to update face images for student: {}", studentId, e);
                throw new RuntimeException("Failed to update face images", e);
            }
        }

        // Refresh the entity to get the updated face images from database
        entityManager.refresh(student);
        logger.info("Student updated successfully: {}", studentId);

        StudentDTO dto = mapToStudentSummaryDTO(student, Collections.emptyList());
        return new StudentFaceProcessingResult(dto, faceProcessingSummary);
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
     * @param courseId  optional course filter
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
                    ids -> !ids.isEmpty() ? enrollmentRepository.findByUserIdInAndIsActive(ids, true)
                            : Collections.emptyList(),
                    SectionEnrollment::getUserId);

            // If section filter is specified, we need to check sections
            if (sectionId != null) {
                students = students.stream()
                        .filter(student -> {
                            List<SectionEnrollment> enrollments = enrollmentsByStudent.getOrDefault(student.getId(),
                                    Collections.emptyList());
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
                            List<SectionEnrollment> enrollments = enrollmentsByStudent.getOrDefault(student.getId(),
                                    Collections.emptyList());
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
                AttendanceRecord::getUserId);

        // Map to lightweight DTOs (stats only, no attendance records)
        return students.stream()
                .map(student -> {
                    List<AttendanceRecord> records = attendanceByStudent.getOrDefault(student.getId(),
                            Collections.emptyList());

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
                        AttendanceSession::getSectionId));

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
        dto.setFaceImages(
                student.getFaceImages() != null ? new ArrayList<>(student.getFaceImages()) : Collections.emptyList());

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
    private StudentDTO mapToStudentDTO(User student, List<AttendanceRecord> attendanceRecords,
            List<SectionEnrollment> enrollments) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setDisplayId(student.getNormalizedStudentId()); // Tell, don't ask!
        dto.setEmail(student.getEmail());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEnabled(student.getEnabled());
        dto.setFaceImages(
                student.getFaceImages() != null ? new ArrayList<>(student.getFaceImages()) : Collections.emptyList());

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
