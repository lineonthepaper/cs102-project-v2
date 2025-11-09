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
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    public StudentDTO createStudent(CreateStudentRequest request) {
        logger.info("createStudent called - Name: {} {}, Email received: {}",
                request.getFirstName(), request.getLastName(), request.getEmail());

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
            String sql = "INSERT INTO users (email, first_name, last_name, face_images, face_profiles, is_student, is_instructor, is_ta, enabled, created_at) "
                    +
                    "VALUES (:email, :firstName, :lastName, CAST(:faceImages AS jsonb), CAST(:faceProfiles AS jsonb), true, false, false, true, CURRENT_TIMESTAMP) "
                    +
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

            logger.info("Successfully created student: {} {} ({})", request.getFirstName(), request.getLastName(),
                    generatedId);
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

    public Map<String, Object> importStudentsFromZip(MultipartFile zipFile) throws IOException {
        Path tempDir = Files.createTempDirectory("student_import_");

        try {
            Path extractDir = tempDir.resolve("extracted");
            Files.createDirectories(extractDir);
            extractZipFile(zipFile.getInputStream(), extractDir);

            Path dataFile = findSingleDataFile(extractDir);

            List<CreateStudentRequest> students;
            if (dataFile.toString().toLowerCase().endsWith(".csv")) {
                students = parseCsvWithImages(dataFile, extractDir);
            } else if (dataFile.toString().toLowerCase().endsWith(".xlsx")) {
                students = parseXlsxWithImages(dataFile, extractDir);
            } else {
                throw new IllegalArgumentException("Unsupported file format");
            }

            List<StudentDTO> importedStudents = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (CreateStudentRequest studentRequest : students) {
                try {
                    StudentDTO student = createStudent(studentRequest);
                    importedStudents.add(student);
                } catch (Exception e) {
                    String errorMsg = String.format("Failed to import %s %s: %s",
                            studentRequest.getFirstName(),
                            studentRequest.getLastName(),
                            e.getMessage());
                    logger.error(errorMsg);
                    errors.add(errorMsg);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Successfully imported %d out of %d students",
                    importedStudents.size(), students.size()));
            response.put("imported", importedStudents.size());
            response.put("total", students.size());
            response.put("students", importedStudents);
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            return response;

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void extractZipFile(InputStream zipInputStream, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = destDir.resolve(entry.getName()).normalize();

                if (!filePath.startsWith(destDir)) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());

                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private Path findSingleDataFile(Path extractDir) throws IOException {
        List<Path> csvFiles = Files.walk(extractDir, 3)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String pathStr = p.toString().toLowerCase();
                    return (pathStr.endsWith(".csv") || pathStr.endsWith(".xlsx"))
                            && !pathStr.contains("__MACOSX")
                            && !pathStr.contains("/.")
                            && !p.getFileName().toString().startsWith(".");
                })
                .collect(Collectors.toList());

        if (csvFiles.isEmpty()) {
            throw new IllegalArgumentException("ZIP must contain at least one CSV or XLSX file");
        }

        if (csvFiles.size() > 1) {
            logger.error("Found {} CSV files:", csvFiles.size());
            csvFiles.forEach(f -> logger.error("  - {}", f));

            throw new IllegalArgumentException(
                    String.format("ZIP must contain exactly 1 CSV or XLSX file, found %d", csvFiles.size()));
        }

        return csvFiles.get(0);
    }

    private List<CreateStudentRequest> parseCsvWithImages(Path csvFile, Path extractDir)
            throws IOException {

        List<CreateStudentRequest> students = new ArrayList<>();
        List<String> lines = Files.readAllLines(csvFile);

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty())
                continue;

            String[] columns = parseCSVLine(line);
            if (columns.length < 3) {
                logger.warn("Skipping invalid CSV line {}: {}", i + 1, line);
                continue;
            }

            String firstName = columns[0].trim();
            String lastName = columns[1].trim();
            String email = columns[2].trim();

            logger.info("Parsed CSV - Name: {} {}, Email from CSV: {}", firstName, lastName, email);

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                logger.warn("Skipping line {} with empty fields", i + 1);
                continue;
            }

            String folderName = firstName + " " + lastName;

            validateStudentFolder(extractDir, folderName, i + 1);

            List<String> imageBase64List = findAndProcessStudentImages(extractDir, folderName);

            CreateStudentRequest request = new CreateStudentRequest();
            request.setFirstName(firstName);
            request.setLastName(lastName);
            request.setEmail(email);
            request.setFaceImages(imageBase64List);

            students.add(request);
        }

        if (students.isEmpty()) {
            throw new IllegalArgumentException("No valid student records found in CSV");
        }

        return students;
    }

    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

    private List<CreateStudentRequest> parseXlsxWithImages(Path xlsxFile, Path extractDir)
            throws IOException {

        List<CreateStudentRequest> students = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(xlsxFile.toFile());
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                Cell firstNameCell = row.getCell(0);
                Cell lastNameCell = row.getCell(1);
                Cell emailCell = row.getCell(2);

                if (firstNameCell == null || lastNameCell == null || emailCell == null) {
                    logger.warn("Skipping row {} with empty cells", i + 1);
                    continue;
                }

                String firstName = getCellValueAsString(firstNameCell).trim();
                String lastName = getCellValueAsString(lastNameCell).trim();
                String email = getCellValueAsString(emailCell).trim();

                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                    logger.warn("Skipping row {} with empty fields", i + 1);
                    continue;
                }

                String folderName = firstName + " " + lastName;
                validateStudentFolder(extractDir, folderName, i + 1);
                List<String> imageBase64List = findAndProcessStudentImages(extractDir, folderName);

                CreateStudentRequest request = new CreateStudentRequest();
                request.setFirstName(firstName);
                request.setLastName(lastName);
                request.setEmail(email);
                request.setFaceImages(imageBase64List);

                students.add(request);
            }
        }

        if (students.isEmpty()) {
            throw new IllegalArgumentException("No valid student records found in XLSX");
        }

        return students;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private List<String> findAndProcessStudentImages(Path extractDir, String folderName)
            throws IOException {

        List<String> imageBase64List = new ArrayList<>();

        Path studentFolder = findStudentFolder(extractDir, folderName);

        if (studentFolder != null && Files.exists(studentFolder) && Files.isDirectory(studentFolder)) {
            List<Path> imageFiles = Files.list(studentFolder)
                    .filter(Files::isRegularFile)
                    .filter(p -> isImageFile(p.getFileName().toString()))
                    .sorted()
                    .collect(Collectors.toList());

            // Validate: max 8 images
            if (imageFiles.size() > 8) {
                throw new IllegalArgumentException(
                        String.format("Student '%s' has %d images (max 8 allowed)",
                                folderName, imageFiles.size()));
            }

            // Convert images to Base64
            for (Path imageFile : imageFiles) {
                try {
                    byte[] imageBytes = Files.readAllBytes(imageFile);
                    String base64 = Base64.getEncoder().encodeToString(imageBytes);
                    imageBase64List.add(base64);
                } catch (IOException e) {
                    logger.error("Failed to read image file: {}", imageFile, e);
                }
            }
        }

        return imageBase64List;
    }

    private Path findStudentFolder(Path extractDir, String folderName) throws IOException {
        logger.info("Searching for folder: '{}'", folderName);

        try (var stream = Files.walk(extractDir, 3)) {
            List<Path> matchingFolders = stream
                    .filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith("__MACOSX"))
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(folderName))
                    .collect(Collectors.toList());

            if (matchingFolders.isEmpty()) {
                logger.warn("No folder found for: '{}'", folderName);
                return null;
            }

            if (matchingFolders.size() > 1) {
                logger.warn("Multiple folders found for '{}': {}", folderName, matchingFolders);
            }

            Path found = matchingFolders.get(0);
            logger.info("Found folder: {}", found);
            return found;
        }
    }

    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".png") ||
                lower.endsWith(".gif") ||
                lower.endsWith(".bmp") ||
                lower.endsWith(".webp");
    }

    private void deleteDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.warn("Failed to delete: {}", path);
                            }
                        });
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup directory: {}", directory, e);
        }
    }

    private void validateStudentFolder(Path extractDir, String folderName, int lineNumber)
            throws IOException {

        Path studentFolder = findStudentFolder(extractDir, folderName);

        if (studentFolder == null || !Files.exists(studentFolder)) {
            throw new IllegalArgumentException(
                    String.format("CSV line %d: No folder found for student '%s'", lineNumber, folderName));
        }

        if (!Files.isDirectory(studentFolder)) {
            throw new IllegalArgumentException(
                    String.format("CSV line %d: '%s' is not a folder", lineNumber, folderName));
        }

        List<Path> imageFiles = Files.list(studentFolder)
                .filter(Files::isRegularFile)
                .filter(p -> isImageFile(p.getFileName().toString()))
                .collect(Collectors.toList());

        if (imageFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("CSV line %d: Folder '%s' contains no valid images", lineNumber, folderName));
        }

        if (imageFiles.size() > 8) {
            throw new IllegalArgumentException(
                    String.format("CSV line %d: Folder '%s' has %d images (max 8 allowed)",
                            lineNumber, folderName, imageFiles.size()));
        }
    }
}
