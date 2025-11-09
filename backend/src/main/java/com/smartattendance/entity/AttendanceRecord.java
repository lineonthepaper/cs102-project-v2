package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.time.Duration;
import com.smartattendance.util.converter.AttendanceStatusConverter;

@Getter 
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private AttendanceSession attendanceSession;

    @Setter
    @Column(name = "status")
    @Convert(converter = AttendanceStatusConverter.class)
    private AttendanceStatus status;  

    @Setter
    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Setter
    @Column(name = "checkout_time")
    private LocalDateTime checkoutTime;

    @Column(name = "notes")
    private String notes;

    @Setter
    @Column(name = "is_automatic")
    private boolean isAutomatic;

    @Setter
    @Column(name = "confidence_level") 
    private double confidenceLevel;
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.userId = userId;
    }
    
    public void setSessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be a positive number");
        }
        this.sessionId = sessionId;
    }
    
    public void setAttendanceSession(AttendanceSession attendanceSession) {
        this.attendanceSession = attendanceSession;
    }
    
    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : null;
    }

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====
    
    /**
     * Check if this attendance record indicates the student was late.
     */
    public boolean isLate() {
        return status != null && status.isLate();
    }
    
    /**
     * Check if this attendance record indicates the student was present (on time or late).
     */
    public boolean isPresent() {
        return status != null && status.isPresent();
    }
    
    /**
     * Check if this attendance record indicates the student was absent.
     */
    public boolean isAbsent() {
        return status != null && status.isAbsent();
    }
    
    /**
     * Check if the student has checked in.
     */
    public boolean hasCheckedIn() {
        return checkinTime != null;
    }
    
    /**
     * Check if the student has checked out.
     */
    public boolean hasCheckedOut() {
        return checkoutTime != null;
    }
    
    /**
     * Mark the student as present.
     * @param time the check-in time
     */
    public void markPresent(LocalDateTime time) {
        this.checkinTime = time;
        this.status = AttendanceStatus.PRESENT;  // Type-safe enum
    }
    
    /**
     * Mark the student as late.
     * @param time the check-in time
     */
    public void markLate(LocalDateTime time) {
        this.checkinTime = time;
        this.status = AttendanceStatus.LATE;  // Type-safe enum
    }
    
    /**
     * Mark the student as absent.
     */
    public void markAbsent() {
        this.status = AttendanceStatus.ABSENT;  // Type-safe enum
        this.checkinTime = null;
        this.checkoutTime = null;
    }
    
    /**
     * Record check-out time.
     * @param time the check-out time
     */
    public void checkOut(LocalDateTime time) {
        if (!hasCheckedIn()) {
            throw new IllegalStateException("Cannot check out before checking in");
        }
        this.checkoutTime = time;
    }
    
    /**
     * Calculate the duration of attendance (time between check-in and check-out).
     * @return duration in minutes, or null if not checked out yet
     */
    public Long getAttendanceDurationMinutes() {
        if (checkinTime == null || checkoutTime == null) {
            return null;
        }
        return Duration.between(checkinTime, checkoutTime).toMinutes();
    }
}

