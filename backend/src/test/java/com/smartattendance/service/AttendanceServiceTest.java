package com.smartattendance.service;

import com.smartattendance.dto.request.CreateAttendanceSessionRequest;
import com.smartattendance.dto.request.MarkAttendanceRequest;
import com.smartattendance.dto.response.AttendanceRecordResponseDTO;
import com.smartattendance.dto.response.AttendanceSessionResponseDTO;
import com.smartattendance.entity.AttendanceRecord;
import com.smartattendance.entity.AttendanceSession;
import com.smartattendance.repository.AttendanceRecordRepository;
import com.smartattendance.repository.AttendanceSessionRepository;
import com.smartattendance.repository.SectionEnrollmentRepository;
import com.smartattendance.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceSessionRepository sessionRepository;

    @Mock
    private AttendanceRecordRepository recordRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SectionEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private AttendanceSession testSession;
    private AttendanceRecord testRecord;

    @BeforeEach
    void setUp() {
        testSession = new AttendanceSession();
        testSession.setId(1L);
        testSession.setSectionId(1L);
        testSession.setSessionDate(LocalDate.of(2025, 10, 24));
        testSession.setScheduledStartTime(LocalTime.of(9, 0));
        testSession.setScheduledEndTime(LocalTime.of(10, 30));
        testSession.setStatus("SCHEDULED");

        testRecord = new AttendanceRecord();
        testRecord.setId(1L);
        testRecord.setSessionId(1L);
        testRecord.setUserId("S0000001");
        testRecord.setStatus("PRESENT");
    }

    @Test
    void getAllSessions_ShouldReturnListOfSessions() {
        // Arrange
        when(sessionRepository.findAll()).thenReturn(Arrays.asList(testSession));
        when(sectionRepository.findById(any())).thenReturn(Optional.empty());

        // Act
        List<AttendanceSessionResponseDTO> result = attendanceService.getAllSessions();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(sessionRepository, times(1)).findAll();
    }

    @Test
    void createSession_WithValidData_ShouldReturnCreatedSession() {
        // Arrange
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest();
        request.setSectionId(1L);
        request.setSessionDate("2025-10-24");
        request.setScheduledStartTime("09:00:00");
        request.setScheduledEndTime("10:30:00");
        request.setStatus("SCHEDULED");
        request.setNotes("Test session");

        when(sessionRepository.save(any(AttendanceSession.class))).thenReturn(testSession);
        when(sectionRepository.findById(any())).thenReturn(Optional.empty());

        // Act
        AttendanceSessionResponseDTO result = attendanceService.createSession(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(sessionRepository, times(1)).save(any(AttendanceSession.class));
    }

    @Test
    void updateSession_WithValidId_ShouldReturnUpdatedSession() {
        // Arrange
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest();
        request.setSectionId(1L);
        request.setSessionDate("2025-10-24");
        request.setScheduledStartTime("10:00:00");
        request.setScheduledEndTime("11:30:00");
        request.setStatus("COMPLETED");
        request.setNotes("Updated session");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(AttendanceSession.class))).thenReturn(testSession);
        when(sectionRepository.findById(any())).thenReturn(Optional.empty());

        // Act
        AttendanceSessionResponseDTO result = attendanceService.updateSession(1L, request);

        // Assert
        assertNotNull(result);
        verify(sessionRepository, times(1)).findById(1L);
        verify(sessionRepository, times(1)).save(any(AttendanceSession.class));
    }

    @Test
    void updateSession_WithInvalidId_ShouldThrowException() {
        // Arrange
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest();
        request.setSectionId(1L);
        request.setSessionDate("2025-10-24");
        request.setScheduledStartTime("09:00:00");
        request.setScheduledEndTime("10:30:00");
        request.setStatus("SCHEDULED");

        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            attendanceService.updateSession(999L, request);
        });
        verify(sessionRepository, times(1)).findById(999L);
        verify(sessionRepository, never()).save(any(AttendanceSession.class));
    }

    @Test
    void getSessionRecords_ShouldReturnListOfRecords() {
        // Arrange
        AttendanceRecord record2 = new AttendanceRecord();
        record2.setId(2L);
        record2.setSessionId(1L);
        record2.setUserId("S0000002");
        record2.setStatus("LATE");

        when(recordRepository.findBySessionId(1L)).thenReturn(Arrays.asList(testRecord, record2));

        // Act
        List<AttendanceRecordResponseDTO> result = attendanceService.getSessionRecords(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(recordRepository, times(1)).findBySessionId(1L);
    }

    @Test
    void markAttendance_WithNewRecord_ShouldCreateRecord() {
        // Arrange
        MarkAttendanceRequest request = new MarkAttendanceRequest();
        request.setSessionId(1L);
        request.setUserId("S0000001");
        request.setStatus("PRESENT");
        request.setCheckinTime(java.time.LocalDateTime.now().toString());

        when(recordRepository.findBySessionIdAndUserId(1L, "S0000001")).thenReturn(Optional.empty());
        when(recordRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // Act
        AttendanceRecordResponseDTO result = attendanceService.markAttendance(request);

        // Assert
        assertNotNull(result);
        assertEquals("S0000001", result.getUserId());
        verify(recordRepository, times(1)).findBySessionIdAndUserId(1L, "S0000001");
        verify(recordRepository, times(1)).save(any(AttendanceRecord.class));
    }

    @Test
    void markAttendance_WithExistingRecord_ShouldUpdateRecord() {
        // Arrange
        MarkAttendanceRequest request = new MarkAttendanceRequest();
        request.setSessionId(1L);
        request.setUserId("S0000001");
        request.setStatus("LATE");
        request.setCheckinTime(java.time.LocalDateTime.now().toString());

        when(recordRepository.findBySessionIdAndUserId(1L, "S0000001")).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // Act
        AttendanceRecordResponseDTO result = attendanceService.markAttendance(request);

        // Assert
        assertNotNull(result);
        verify(recordRepository, times(1)).findBySessionIdAndUserId(1L, "S0000001");
        verify(recordRepository, times(1)).save(any(AttendanceRecord.class));
    }
}

