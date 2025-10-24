package com.smartattendance.service;

import com.smartattendance.dto.request.CreateAttendanceSessionRequest;
import com.smartattendance.dto.request.MarkAttendanceRequest;
import com.smartattendance.dto.response.AttendanceRecordResponseDTO;
import com.smartattendance.dto.response.AttendanceSessionResponseDTO;
import com.smartattendance.entity.*;
import com.smartattendance.repository.*;
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

    public AttendanceService(
            AttendanceSessionRepository sessionRepository,
            AttendanceRecordRepository recordRepository,
            SectionRepository sectionRepository,
            SectionEnrollmentRepository enrollmentRepository) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.sectionRepository = sectionRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AttendanceSessionResponseDTO> getAllSessions() {
        return sessionRepository.findAll().stream()
                .<AttendanceSessionResponseDTO>map(this::mapToSessionDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceSessionResponseDTO createSession(CreateAttendanceSessionRequest request) {
        AttendanceSession session = new AttendanceSession();
        session.setSectionId(request.getSectionId());
        session.setSessionDate(LocalDate.parse(request.getSessionDate()));
        session.setScheduledStartTime(LocalTime.parse(request.getScheduledStartTime()));
        session.setScheduledEndTime(LocalTime.parse(request.getScheduledEndTime()));
        session.setStatus(request.getStatus());
        session.setNotes(request.getNotes());

        AttendanceSession savedSession = sessionRepository.save(session);
        return mapToSessionDTO(savedSession);
    }

    @Transactional
    public AttendanceSessionResponseDTO updateSession(Long id, CreateAttendanceSessionRequest request) {
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setSectionId(request.getSectionId());
        session.setSessionDate(LocalDate.parse(request.getSessionDate()));
        session.setScheduledStartTime(LocalTime.parse(request.getScheduledStartTime()));
        session.setScheduledEndTime(LocalTime.parse(request.getScheduledEndTime()));
        session.setStatus(request.getStatus());
        session.setNotes(request.getNotes());

        AttendanceSession updatedSession = sessionRepository.save(session);
        return mapToSessionDTO(updatedSession);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponseDTO> getSessionRecords(Long sessionId) {
        List<AttendanceRecord> records = recordRepository.findBySessionId(sessionId);
        return records.stream()
                .<AttendanceRecordResponseDTO>map(this::mapToRecordDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceRecordResponseDTO markAttendance(MarkAttendanceRequest request) {
        // Check if record already exists
        AttendanceRecord record = recordRepository
                .findBySessionIdAndUserId(request.getSessionId(), request.getUserId())
                .orElse(new AttendanceRecord());

        record.setSessionId(request.getSessionId());
        record.setUserId(request.getUserId());
        record.setStatus(request.getStatus());
        record.setNotes(request.getNotes());

        if (request.getCheckinTime() != null && !request.getCheckinTime().isEmpty()) {
            record.setCheckinTime(LocalDateTime.parse(request.getCheckinTime(), 
                DateTimeFormatter.ISO_DATE_TIME));
        }

        AttendanceRecord savedRecord = recordRepository.save(record);
        return mapToRecordDTO(savedRecord);
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
            sectionInfo.setSemester(section.getSemester());
            sectionInfo.setDayOfWeek(dayNumberToName(section.getMeetingDay()));
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
        dto.setStatus(record.getStatus());
        dto.setCheckinTime(record.getCheckinTime() != null ? 
            record.getCheckinTime().toString() : null);
        dto.setCheckoutTime(record.getCheckoutTime() != null ? 
            record.getCheckoutTime().toString() : null);
        dto.setNotes(record.getNotes());
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
}

