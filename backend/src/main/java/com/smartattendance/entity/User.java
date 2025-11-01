package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter 
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(nullable = false, updatable = false, length = 255)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "auth_id")
    private String authId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "is_student", nullable = false)
    private Boolean isStudent = false;

    @Column(name = "is_instructor", nullable = false)
    private Boolean isInstructor = false;

    @Column(name = "is_ta", nullable = false)
    private Boolean isTA = false;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "face_images", columnDefinition = "jsonb", nullable = false)
    private List<String> faceImages = new ArrayList<>();
    
    // ===== VALIDATED SETTERS (ISP Fix) =====
    
    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.id = id.trim();
    }
    
    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        // Basic email validation
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.email = email.toLowerCase().trim();
    }
    
    public void setAuthId(String authId) {
        this.authId = authId != null ? authId.trim() : null;
    }
    
    public void setFirstName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (firstName.length() < 2) {
            throw new IllegalArgumentException("First name must be at least 2 characters");
        }
        this.firstName = firstName.trim();
    }
    
    public void setLastName(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        if (lastName.length() < 2) {
            throw new IllegalArgumentException("Last name must be at least 2 characters");
        }
        this.lastName = lastName.trim();
    }
    
    public void setIsStudent(Boolean isStudent) {
        this.isStudent = isStudent != null ? isStudent : false;
    }
    
    public void setIsInstructor(Boolean isInstructor) {
        this.isInstructor = isInstructor != null ? isInstructor : false;
    }
    
    public void setIsTA(Boolean isTA) {
        this.isTA = isTA != null ? isTA : false;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled != null ? enabled : true;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setFaceImages(List<String> faceImages) {
        if (faceImages == null) {
            this.faceImages = new ArrayList<>();
            return;
        }
        if (faceImages.size() > 8) {
            throw new IllegalArgumentException("Face images cannot exceed 8 entries");
        }

        List<String> sanitizedImages = new ArrayList<>();
        for (String image : faceImages) {
            if (image == null || image.isBlank()) {
                throw new IllegalArgumentException("Face image data cannot be null or blank");
            }
            sanitizedImages.add(image.trim());
        }
        this.faceImages = sanitizedImages;
    }

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====

    /**
     * Determines the user's primary role based on their role flags.
     * Priority: INSTRUCTOR > TA > STUDENT
     * 
     * @return the user's primary role
     */
    public UserRole getRole() {
        if (isInstructor) return UserRole.INSTRUCTOR;
        if (isTA) return UserRole.TA;
        return UserRole.STUDENT;
    }
    
    /**
     * Get the user's full name.
     * @return first name + last name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Get the user's formatted display name (Last, First).
     * @return last name, first name
     */
    public String getDisplayName() {
        return lastName + ", " + firstName;
    }
    
    /**
     * Check if this user has student privileges.
     */
    public boolean canAccessStudentFeatures() {
        return isStudent || isTA || isInstructor;
    }
    
    /**
     * Check if this user has teaching privileges (TA or Instructor).
     */
    public boolean canTeach() {
        return isTA || isInstructor;
    }
    
    /**
     * Check if this user has administrative privileges.
     */
    public boolean hasAdminPrivileges() {
        return isInstructor;
    }
    
    /**
     * Make this user a student.
     */
    public void makeStudent() {
        this.isStudent = true;
    }
    
    /**
     * Make this user an instructor.
     */
    public void makeInstructor() {
        this.isInstructor = true;
        this.isStudent = false; // Instructors aren't students
    }
    
    /**
     * Make this user a TA.
     */
    public void makeTA() {
        this.isTA = true;
    }
    
    /**
     * Enable this user account.
     */
    public void enable() {
        this.enabled = true;
    }
    
    /**
     * Disable this user account.
     */
    public void disable() {
        this.enabled = false;
    }
    
    /**
     * Check if the user account is active.
     */
    public boolean isActive() {
        return enabled != null && enabled;
    }
    
    /**
     * Get a normalized display ID for students (e.g., S0000001).
     * @return formatted student ID
     */
    public String getNormalizedStudentId() {
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
    
    /**
     * Calculate attendance statistics for this student.
     * @param attendanceRecords the student's attendance records
     * @return an AttendanceStatistics object
     */
    public AttendanceStatistics calculateAttendanceStats(List<AttendanceRecord> attendanceRecords) {
        if (attendanceRecords == null || attendanceRecords.isEmpty()) {
            return new AttendanceStatistics(0, 0, 0, 0, 0);
        }
        
        int totalSessions = attendanceRecords.size();
        int lateSessions = (int) attendanceRecords.stream()
                .filter(AttendanceRecord::isLate)
                .count();
        int presentSessions = (int) attendanceRecords.stream()
                .filter(AttendanceRecord::isPresent)
                .count();
        
        int attendanceRate = Math.round((float) presentSessions / totalSessions * 100);
        int punctualityRate = Math.round((float) (totalSessions - lateSessions) / totalSessions * 100);
        
        return new AttendanceStatistics(totalSessions, presentSessions, lateSessions, attendanceRate, punctualityRate);
    }
    
    /**
     * Inner class to hold attendance statistics.
     */
    public static class AttendanceStatistics {
        public final int totalSessions;
        public final int presentSessions;
        public final int lateSessions;
        public final int attendanceRate;
        public final int punctualityRate;
        
        public AttendanceStatistics(int totalSessions, int presentSessions, int lateSessions, 
                                   int attendanceRate, int punctualityRate) {
            this.totalSessions = totalSessions;
            this.presentSessions = presentSessions;
            this.lateSessions = lateSessions;
            this.attendanceRate = attendanceRate;
            this.punctualityRate = punctualityRate;
        }
    }
}
