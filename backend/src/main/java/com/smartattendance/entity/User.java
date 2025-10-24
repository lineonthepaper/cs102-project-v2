package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
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
}
