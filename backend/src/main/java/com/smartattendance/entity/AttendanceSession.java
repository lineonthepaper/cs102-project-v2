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
            !upperStatus.equals(AttendanceConstants.SESSION_STATUS_ENDED) &&
            !upperStatus.equals(AttendanceConstants.SESSION_STATUS_CLOSED) &&
            !upperStatus.equals(AttendanceConstants.SESSION_STATUS_COMPLETED) &&
            !upperStatus.equals(AttendanceConstants.SESSION_STATUS_ARCHIVED) &&
            !upperStatus.equals(AttendanceConstants.SESSION_STATUS_CANCELLED)) {
            throw new IllegalArgumentException("Invalid status: " + status + ". Must be SCHEDULED, ACTIVE, ENDED, CLOSED, COMPLETED, ARCHIVED, or CANCELLED");
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
            || AttendanceConstants.SESSION_STATUS_CLOSED.equalsIgnoreCase(status)
            || AttendanceConstants.SESSION_STATUS_COMPLETED.equalsIgnoreCase(status)
            || AttendanceConstants.SESSION_STATUS_ARCHIVED.equalsIgnoreCase(status)
            || AttendanceConstants.SESSION_STATUS_CANCELLED.equalsIgnoreCase(status);
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
     * Reopen a completed/closed session to allow editing attendance.
     */
    public void reopen() {
        if (!hasEnded()) {
            throw new IllegalStateException("Cannot reopen a session that has not ended");
        }
        this.status = AttendanceConstants.SESSION_STATUS_ACTIVE;
    }
    
    /**
     * Archive a session to prevent further modifications.
     */
    public void archive() {
        if (!hasEnded()) {
            throw new IllegalStateException("Cannot archive a session that has not ended");
        }
        this.status = AttendanceConstants.SESSION_STATUS_ARCHIVED;
    }
    
    /**
     * Cancel a session that hasn't started yet.
     */
    public void cancel() {
        if (hasEnded() || isActive()) {
            throw new IllegalStateException("Cannot cancel a session that has already started or ended");
        }
        this.status = AttendanceConstants.SESSION_STATUS_CANCELLED;
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
    
    /**
     * Automatically determine the session status based on current date and time.
     * This method should be called when retrieving sessions to ensure correct status display.
     * 
     * Status logic:
     * - If manually ended (ARCHIVED, ENDED, COMPLETED, CANCELLED): Keep as is
     * - If session date is in the future: SCHEDULED
     * - If session date is today and within time window: ACTIVE
     * - If session date is today but after time window: COMPLETED
     * - If session date is in the past: COMPLETED
     * 
     * @return the appropriate status based on current state
     */
    public String getAutomaticStatus() {
        // If already manually ended, keep that status
        if (hasEnded()) {
            return this.status;
        }
        
        // If cancelled, keep cancelled
        if (AttendanceConstants.SESSION_STATUS_CANCELLED.equalsIgnoreCase(this.status)) {
            return this.status;
        }
        
        LocalDate now = LocalDate.now();
        LocalDateTime nowDateTime = LocalDateTime.now();
        
        // Future session
        if (sessionDate != null && sessionDate.isAfter(now)) {
            return AttendanceConstants.SESSION_STATUS_SCHEDULED;
        }
        
        // Past session
        if (sessionDate != null && sessionDate.isBefore(now)) {
            return AttendanceConstants.SESSION_STATUS_COMPLETED;
        }
        
        // Today's session - check time window
        if (sessionDate != null && sessionDate.equals(now)) {
            if (scheduledStartTime != null && scheduledEndTime != null) {
                LocalTime currentTime = nowDateTime.toLocalTime();
                
                // Before start time
                if (currentTime.isBefore(scheduledStartTime)) {
                    return AttendanceConstants.SESSION_STATUS_SCHEDULED;
                }
                
                // After end time
                if (currentTime.isAfter(scheduledEndTime)) {
                    return AttendanceConstants.SESSION_STATUS_COMPLETED;
                }
                
                // Within time window - ACTIVE
                return AttendanceConstants.SESSION_STATUS_ACTIVE;
            }
            
            // No time specified but date is today - consider it ACTIVE
            return AttendanceConstants.SESSION_STATUS_ACTIVE;
        }
        
        // Default fallback
        return this.status != null ? this.status : AttendanceConstants.SESSION_STATUS_SCHEDULED;
    }
    
    /**
     * Update session status to match automatic status.
     * This is useful when you want to persist the computed status.
     */
    public void updateToAutomaticStatus() {
        String computedStatus = getAutomaticStatus();
        this.status = computedStatus;
    }
}

