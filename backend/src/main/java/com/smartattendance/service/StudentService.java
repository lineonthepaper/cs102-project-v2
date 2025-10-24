package com.smartattendance.service;

import com.smartattendance.dto.request.CreateStudentRequest;
import com.smartattendance.dto.request.UpdateEnrollmentRequest;
import com.smartattendance.dto.response.*;
import com.smartattendance.entity.*;
import com.smartattendance.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;

    public StudentService(
            UserRepository userRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            SectionEnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            SectionRepository sectionRepository) {
        this.userRepository = userRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        // Fetch all students
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getIsStudent())
                .collect(Collectors.toList());

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
        // Check if student already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("A student with this email address already exists in the system.");
        }

        // Use native SQL to insert student so database trigger can generate the ID
        String sql = "INSERT INTO users (email, first_name, last_name, is_student, is_instructor, is_ta, enabled, created_at) " +
                     "VALUES (:email, :firstName, :lastName, true, false, false, true, CURRENT_TIMESTAMP) " +
                     "RETURNING id";
        
        String generatedId = (String) entityManager.createNativeQuery(sql)
                .setParameter("email", request.getEmail())
                .setParameter("firstName", request.getFirstName())
                .setParameter("lastName", request.getLastName())
                .getSingleResult();

        // Fetch the newly created student
        User savedStudent = userRepository.findById(generatedId)
                .orElseThrow(() -> new RuntimeException("Failed to fetch created student"));

        return mapToStudentDTO(savedStudent, Collections.emptyList(), Collections.emptyList());
    }

    @Transactional
    public void updateEnrollments(String studentId, UpdateEnrollmentRequest request) {
        // Verify student exists
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Set<Long> newSectionIds = new HashSet<>(request.getSectionIds());
        
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

        // Map attendance records
        dto.setAttendanceRecords(attendanceRecords.stream()
                .<AttendanceRecordDTO>map(this::mapToAttendanceRecordDTO)
                .collect(Collectors.toList()));

        // Map enrollments
        dto.setEnrollments(enrollments.stream()
                .<EnrollmentDTO>map(this::mapToEnrollmentDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private EnrollmentDTO mapToEnrollmentDTO(SectionEnrollment enrollment) {
        EnrollmentDTO dto = new EnrollmentDTO();
        dto.setId(enrollment.getId());
        dto.setUserId(enrollment.getUserId());
        dto.setSectionId(enrollment.getSectionId());
        dto.setIsActive(enrollment.getIsActive());
        dto.setEnrolledAt(enrollment.getEnrolledAt());

        if (enrollment.getSection() != null) {
            dto.setSection(mapToSectionDTO(enrollment.getSection()));
        }

        return dto;
    }

    private SectionDTO mapToSectionDTO(Section section) {
        SectionDTO dto = new SectionDTO();
        dto.setId(section.getId());
        dto.setSectionCode(section.getSectionCode());
        dto.setCourseId(section.getCourseId());
        dto.setYear(section.getYear());
        dto.setSemester(section.getSemester());
        dto.setMeetingDay(dayNumberToName(section.getMeetingDay()));
        dto.setStartTime(section.getStartTime());
        dto.setEndTime(section.getEndTime());
        dto.setLocation(section.getLocation());

        if (section.getCourse() != null) {
            dto.setCourse(mapToCourseDTO(section.getCourse()));
        }

        return dto;
    }
    
    private String dayNumberToName(Integer dayNumber) {
        if (dayNumber == null) return null;
        return switch (dayNumber) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> null;
        };
    }

    private CourseDTO mapToCourseDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setCode(course.getCode());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        return dto;
    }

    private AttendanceRecordDTO mapToAttendanceRecordDTO(AttendanceRecord record) {
        AttendanceRecordDTO dto = new AttendanceRecordDTO();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setSessionId(record.getSessionId());
        dto.setStatus(record.getStatus());
        dto.setCheckinTime(record.getCheckinTime());
        dto.setCheckoutTime(record.getCheckoutTime());

        if (record.getAttendanceSession() != null) {
            dto.setAttendanceSession(mapToAttendanceSessionDTO(record.getAttendanceSession()));
        }

        return dto;
    }

    private AttendanceSessionDTO mapToAttendanceSessionDTO(AttendanceSession session) {
        AttendanceSessionDTO dto = new AttendanceSessionDTO();
        dto.setId(session.getId());
        dto.setSectionId(session.getSectionId());
        dto.setSessionDate(session.getSessionDate());
        dto.setScheduledStartTime(session.getScheduledStartTime());
        dto.setScheduledEndTime(session.getScheduledEndTime());
        dto.setStatus(session.getStatus());
        dto.setNotes(session.getNotes());

        if (session.getSection() != null) {
            dto.setSection(mapToSectionDTO(session.getSection()));
        }

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

