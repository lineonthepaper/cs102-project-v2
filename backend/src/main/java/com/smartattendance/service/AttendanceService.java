package com.smartattendance.service;

import com.smartattendance.dto.request.attendance.CreateAttendanceSessionRequest;
import com.smartattendance.dto.request.attendance.MarkAttendanceRequest;
import com.smartattendance.dto.response.attendance.AttendanceRecordResponseDTO;
import com.smartattendance.dto.response.attendance.AttendanceSessionResponseDTO;
import com.smartattendance.entity.*;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.repository.*;
import com.smartattendance.service.strategy.AttendanceStrategyFactory;
import com.smartattendance.service.strategy.AttendanceMarkingStrategy;
import com.smartattendance.util.helper.DateTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final SectionRepository sectionRepository;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final AttendanceStrategyFactory strategyFactory;
    private final SessionRecognitionManager recognitionManager;

    public AttendanceService(
            AttendanceSessionRepository sessionRepository,
            AttendanceRecordRepository recordRepository,
            SectionRepository sectionRepository,
            SectionEnrollmentRepository enrollmentRepository,
            AttendanceStrategyFactory strategyFactory,
            SessionRecognitionManager recognitionManager) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.sectionRepository = sectionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.strategyFactory = strategyFactory;
        this.recognitionManager = recognitionManager;
    }

    @Transactional(readOnly = true)
    public List<AttendanceSessionResponseDTO> getAllSessions() {
        return sessionRepository.findAll().stream()
                .<AttendanceSessionResponseDTO>map(this::mapToSessionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new attendance session.
     * Service is now a thin orchestrator - entity has the behavior.
     */
    @Transactional
    public AttendanceSessionResponseDTO createSession(CreateAttendanceSessionRequest request) {
        try {
            AttendanceSession session = new AttendanceSession();
            session.setSectionId(request.getSectionId());
            session.setSessionDate(LocalDate.parse(request.getSessionDate()));
            session.setScheduledStartTime(LocalTime.parse(request.getScheduledStartTime()));
            session.setScheduledEndTime(LocalTime.parse(request.getScheduledEndTime()));
            session.setStatus(request.getStatus());
            session.setNotes(request.getNotes());
    
            AttendanceSession savedSession = sessionRepository.save(session);
            return mapToSessionDTO(savedSession);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidRequestException("Session could not be created.");
        }
    }

    @Transactional
    public AttendanceSessionResponseDTO updateSession(Long id, CreateAttendanceSessionRequest request) {
        try{
            AttendanceSession session = sessionRepository.findById(id)
                    .orElseThrow(() -> new com.smartattendance.exception.ResourceNotFoundException("AttendanceSession", id.toString()));
    
            String oldStatus = session.getStatus();
            String newStatus = request.getStatus();
            
            session.setSectionId(request.getSectionId());
            session.setSessionDate(LocalDate.parse(request.getSessionDate()));
            session.setScheduledStartTime(LocalTime.parse(request.getScheduledStartTime()));
            session.setScheduledEndTime(LocalTime.parse(request.getScheduledEndTime()));
            session.setStatus(request.getStatus());
            session.setNotes(request.getNotes());
    
            AttendanceSession updatedSession = sessionRepository.save(session);
            
            // Auto-mark absent if session is being closed/ended
            if (!isEndedStatus(oldStatus) && isEndedStatus(newStatus)) {
                autoMarkAbsentForUnmarkedStudents(id, request.getSectionId());
            }
            
            return mapToSessionDTO(updatedSession);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidRequestException("Session could not be updated.");
        }
    }
    
    private boolean isEndedStatus(String status) {
        if (status == null) return false;
        String upper = status.toUpperCase();
        return "ENDED".equals(upper) || "CLOSED".equals(upper) || "COMPLETED".equals(upper);
    }
    
    private void autoMarkAbsentForUnmarkedStudents(Long sessionId, Long sectionId) {
        // Get all enrolled students for this section
        List<String> enrolledStudentIds = enrollmentRepository
                .findBySectionIdAndIsActive(sectionId, true)
                .stream()
                .map(enrollment -> enrollment.getUserId())
                .collect(Collectors.toList());
        
        if (enrolledStudentIds.isEmpty()) {
            System.out.println("[AUTO-ABSENT] No enrolled students for session " + sessionId);
            return;
        }
        
        // Get students who already have attendance records
        List<String> markedStudentIds = recordRepository.findBySessionId(sessionId)
                .stream()
                .map(record -> record.getUserId())
                .collect(Collectors.toList());
        
        // Find unmarked students
        List<String> unmarkedStudentIds = enrolledStudentIds.stream()
                .filter(id -> !markedStudentIds.contains(id))
                .collect(Collectors.toList());
        
        if (unmarkedStudentIds.isEmpty()) {
            System.out.println("[AUTO-ABSENT] All students already marked for session " + sessionId);
            return;
        }
        
        System.out.println("[AUTO-ABSENT] Auto-marking " + unmarkedStudentIds.size() + " students as ABSENT for session " + sessionId);
        
        // Create ABSENT records for unmarked students
        for (String studentId : unmarkedStudentIds) {
            AttendanceRecord record = new AttendanceRecord();
            record.setSessionId(sessionId);
            record.setUserId(studentId);
            record.setStatus(AttendanceStatus.ABSENT);
            record.setCheckinTime(null); // No check-in time for absent
            record.setNotes("Auto-marked absent when session closed");
            recordRepository.save(record);
        }
        
        System.out.println("[AUTO-ABSENT] Successfully marked " + unmarkedStudentIds.size() + " students as ABSENT");
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponseDTO> getSessionRecords(Long sessionId) {
        List<AttendanceRecord> records = recordRepository.findBySessionId(sessionId);
        return records.stream()
                .<AttendanceRecordResponseDTO>map(this::mapToRecordDTO)
                .collect(Collectors.toList());
    }

    /**
     * Mark attendance for a student.
     * Now using Strategy Pattern - eliminates switch statements and follows Open/Closed Principle!
     * 
     * Benefits of Strategy Pattern:
     * - Adding new attendance statuses doesn't require modifying this method
     * - Each status has its own encapsulated behavior
     * - Follows Open/Closed Principle: open for extension, closed for modification
     */
    @Transactional
    public AttendanceRecordResponseDTO markAttendance(MarkAttendanceRequest request) {
        try {
            // Check if record already exists
            AttendanceRecord record = recordRepository
                    .findBySessionIdAndUserId(request.getSessionId(), request.getUserId())
                    .orElse(new AttendanceRecord());
    
            record.setSessionId(request.getSessionId());
            record.setUserId(request.getUserId());
            record.setNotes(request.getNotes());
    
            // Parse checkin time
            LocalDateTime checkinTime = request.getCheckinTime() != null && !request.getCheckinTime().isEmpty()
                    ? LocalDateTime.parse(request.getCheckinTime(), DateTimeFormatter.ISO_DATE_TIME)
                    : LocalDateTime.now();
    
            // Use Strategy Pattern - get the appropriate strategy and execute it
            try {
                AttendanceMarkingStrategy strategy = strategyFactory.getStrategy(request.getStatus());
                strategy.mark(record, checkinTime);
            } catch (IllegalArgumentException e) {
                // Fallback for unknown statuses (backwards compatibility)
                // Convert String to AttendanceStatus
                try {
                    AttendanceStatus status = AttendanceStatus.fromCode(request.getStatus());
                    record.setStatus(status);
                } catch (IllegalArgumentException ex) {
                    // If still invalid, set to null or default
                    record.setStatus(null);
                }
                if (request.getCheckinTime() != null && !request.getCheckinTime().isEmpty()) {
                    record.setCheckinTime(checkinTime);
                }
            }
    
            AttendanceRecord savedRecord = recordRepository.save(record);
            recognitionManager.registerAttendance(request.getSessionId(), request.getUserId(), savedRecord.getStatus());
            return mapToRecordDTO(savedRecord);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidRequestException("Attendance could not be marked.");
        }
    }

    private AttendanceSessionResponseDTO mapToSessionDTO(AttendanceSession session) {
        AttendanceSessionResponseDTO dto = new AttendanceSessionResponseDTO();
        dto.setId(session.getId());
        dto.setSectionId(session.getSectionId());
        dto.setSessionDate(session.getSessionDate().toString());
        dto.setScheduledStartTime(session.getScheduledStartTime().toString());
        dto.setScheduledEndTime(session.getScheduledEndTime().toString());
        dto.setStatus(session.getStatus());
        dto.setNotes(session.getNotes());

        // Fetch section info
        sectionRepository.findById(session.getSectionId()).ifPresent(section -> {
            AttendanceSessionResponseDTO.SectionInfoDTO sectionInfo = 
                new AttendanceSessionResponseDTO.SectionInfoDTO();
            sectionInfo.setSectionCode(section.getSectionCode());
            sectionInfo.setYear(section.getYear());
            // Convert Semester enum to Integer for DTO
            sectionInfo.setSemester(section.getSemester() != null ? section.getSemester().getValue() : null);
            sectionInfo.setDayOfWeek(DateTimeUtils.dayNumberToName(section.getMeetingDay()));
            sectionInfo.setStartTime(section.getStartTime() != null ? section.getStartTime().toString() : null);
            sectionInfo.setEndTime(section.getEndTime() != null ? section.getEndTime().toString() : null);
            sectionInfo.setLocation(section.getLocation());

            if (section.getCourse() != null) {
                AttendanceSessionResponseDTO.CourseInfoDTO courseInfo = 
                    new AttendanceSessionResponseDTO.CourseInfoDTO();
                courseInfo.setCode(section.getCourse().getCode());
                courseInfo.setTitle(section.getCourse().getTitle());
                sectionInfo.setCourse(courseInfo);
            }

            dto.setSection(sectionInfo);
        });

        return dto;
    }

    private AttendanceRecordResponseDTO mapToRecordDTO(AttendanceRecord record) {
        AttendanceRecordResponseDTO dto = new AttendanceRecordResponseDTO();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setSessionId(record.getSessionId());
        // Convert AttendanceStatus enum to String for DTO
        dto.setStatus(record.getStatus() != null ? record.getStatus().getCode() : null);
        dto.setCheckinTime(record.getCheckinTime() != null ? 
            record.getCheckinTime().toString() : null);
        dto.setCheckoutTime(record.getCheckoutTime() != null ? 
            record.getCheckoutTime().toString() : null);
        dto.setNotes(record.getNotes());
        return dto;
    }
}

