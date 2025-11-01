package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import com.smartattendance.util.constants.AttendanceConstants;

@Getter 
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", insertable = false, updatable = false)
    private Section section;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalTime scheduledEndTime;

    @Column(name = "status")
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ===== VALIDATED SETTERS (ISP Fix) =====
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setSectionId(Long sectionId) {
        if (sectionId == null || sectionId <= 0) {
            throw new IllegalArgumentException("Section ID must be a positive number");
        }
        this.sectionId = sectionId;
    }
    
    public void setSection(Section section) {
        this.section = section;
    }
    
    public void setSessionDate(LocalDate sessionDate) {
        if (sessionDate == null) {
            throw new IllegalArgumentException("Session date cannot be null");
        }
        this.sessionDate = sessionDate;
    }
    
    public void setScheduledStartTime(LocalTime scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }
    
    public void setScheduledEndTime(LocalTime scheduledEndTime) {
        if (scheduledEndTime != null && scheduledStartTime != null && scheduledEndTime.isBefore(scheduledStartTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.scheduledEndTime = scheduledEndTime;
    }
    
    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        String upperStatus = status.toUpperCase();
        // Validate against known statuses
        if (!upperStatus.equals(AttendanceConstants.SESSION_STATUS_SCHEDULED) &&
            !upperStatus.equals(AttendanceConstants.SESSION_STATUS_ACTIVE) &&
            !upperStatus.equals(AttendanceConstants.SESSION_STATUS_ENDED)) {
            throw new IllegalArgumentException("Invalid status: " + status + ". Must be SCHEDULED, ACTIVE, or ENDED");
        }
        this.status = upperStatus;
    }
    
    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : null;
    }

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====
    
    /**
     * Check if the session is currently active.
     */
    public boolean isActive() {
        return AttendanceConstants.SESSION_STATUS_ACTIVE.equalsIgnoreCase(status);
    }
    
    /**
     * Check if the session has ended.
     */
    public boolean hasEnded() {
        return AttendanceConstants.SESSION_STATUS_ENDED.equalsIgnoreCase(status) 
            || AttendanceConstants.SESSION_STATUS_CLOSED.equalsIgnoreCase(status);
    }
    
    /**
     * Check if the session is scheduled (not started yet).
     */
    public boolean isScheduled() {
        return AttendanceConstants.SESSION_STATUS_SCHEDULED.equalsIgnoreCase(status) || status == null;
    }
    
    /**
     * Check if a given time is within the session window.
     * @param time the time to check
     * @return true if the time is between scheduled start and end times
     */
    public boolean isWithinSessionWindow(LocalDateTime time) {
        if (scheduledStartTime == null || scheduledEndTime == null) {
            return false;
        }
        LocalTime checkTime = time.toLocalTime();
        return !checkTime.isBefore(scheduledStartTime) && !checkTime.isAfter(scheduledEndTime);
    }
    
    /**
     * Check if a check-in at the given time should be marked as late.
     * Students are late if they check in more than the threshold minutes after scheduled start.
     * @param checkinTime the check-in time
     * @return true if the check-in is late
     */
    public boolean isCheckinLate(LocalDateTime checkinTime) {
        if (scheduledStartTime == null) {
            return false;
        }
        LocalTime checkTime = checkinTime.toLocalTime();
        LocalTime lateThreshold = scheduledStartTime.plusMinutes(AttendanceConstants.LATE_THRESHOLD_MINUTES);
        return checkTime.isAfter(lateThreshold);
    }
    
    /**
     * Check if students can currently check in to this session.
     * @return true if the session is active and not ended
     */
    public boolean canCheckIn() {
        return isActive() && !hasEnded();
    }
    
    /**
     * Start the session.
     */
    public void start() {
        if (hasEnded()) {
            throw new IllegalStateException("Cannot start a session that has already ended");
        }
        this.status = AttendanceConstants.SESSION_STATUS_ACTIVE;
    }
    
    /**
     * End the session.
     */
    public void end() {
        if (!isActive()) {
            throw new IllegalStateException("Cannot end a session that is not active");
        }
        this.status = AttendanceConstants.SESSION_STATUS_ENDED;
    }
    
    /**
     * Check if this session is for today.
     * @return true if the session date is today
     */
    public boolean isToday() {
        return sessionDate != null && sessionDate.equals(LocalDate.now());
    }
    
    /**
     * Check if this session is in the past.
     * @return true if the session date is before today
     */
    public boolean isPast() {
        return sessionDate != null && sessionDate.isBefore(LocalDate.now());
    }
    
    /**
     * Check if this session is in the future.
     * @return true if the session date is after today
     */
    public boolean isFuture() {
        return sessionDate != null && sessionDate.isAfter(LocalDate.now());
    }
}

