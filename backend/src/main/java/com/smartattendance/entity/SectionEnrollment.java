package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.time.Duration;
import com.smartattendance.util.constants.AttendanceConstants;

@Getter 
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "section_enrollments")
public class SectionEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", insertable = false, updatable = false)
    private Section section;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    // ===== VALIDATED SETTERS (ISP Fix) =====
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.userId = userId.trim();
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
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }
    
    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====
    
    /**
     * Check if this enrollment is currently active.
     * 
     * @return true if enrollment is active
     */
    public boolean isCurrentlyActive() {
        return Boolean.TRUE.equals(isActive);
    }
    
    /**
     * Activate this enrollment.
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * Deactivate (drop) this enrollment.
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * Calculate how long the student has been enrolled (in days).
     * 
     * @return duration in days, or null if enrolledAt is null
     */
    public Long getEnrollmentDurationDays() {
        if (enrolledAt == null) {
            return null;
        }
        return Duration.between(enrolledAt, LocalDateTime.now()).toDays();
    }
    
    /**
     * Check if this is a new enrollment (less than the threshold days old).
     * 
     * @return true if enrollment is less than threshold days old
     */
    public boolean isNewEnrollment() {
        Long days = getEnrollmentDurationDays();
        return days != null && days < AttendanceConstants.NEW_ENROLLMENT_DAYS_THRESHOLD;
    }
    
    /**
     * Check if enrollment occurred before a specific date.
     * 
     * @param date the date to compare against
     * @return true if enrolled before the given date
     */
    public boolean wasEnrolledBefore(LocalDateTime date) {
        if (enrolledAt == null || date == null) {
            return false;
        }
        return enrolledAt.isBefore(date);
    }
    
    /**
     * Check if enrollment occurred after a specific date.
     * 
     * @param date the date to compare against
     * @return true if enrolled after the given date
     */
    public boolean wasEnrolledAfter(LocalDateTime date) {
        if (enrolledAt == null || date == null) {
            return false;
        }
        return enrolledAt.isAfter(date);
    }
    
    /**
     * Get the formatted enrollment date.
     * 
     * @return formatted enrollment date string
     */
    public String getFormattedEnrollmentDate() {
        if (enrolledAt == null) {
            return AttendanceConstants.UNKNOWN;
        }
        return enrolledAt.toLocalDate().toString();
    }
    
    /**
     * Check if student can unenroll (drop the course).
     * Business rule: Can only drop if enrollment is currently active.
     * 
     * @return true if student can drop
     */
    public boolean canDrop() {
        return isCurrentlyActive();
    }
    
    /**
     * Check if enrollment can be reactivated.
     * Business rule: Can reactivate if currently inactive.
     * 
     * @return true if can be reactivated
     */
    public boolean canReactivate() {
        return !isCurrentlyActive();
    }
}

