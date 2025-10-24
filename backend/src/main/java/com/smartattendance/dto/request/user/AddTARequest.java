package com.smartattendance.dto.request.user;

import com.smartattendance.util.constants.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for adding a new TA.
 * Contains validation rules for all required fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTARequest {
    
    @NotBlank(message = ValidationConstants.EMAIL_REQUIRED_MESSAGE)
    @Email(message = ValidationConstants.EMAIL_INVALID_MESSAGE)
    private String email;
    
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(student|instructor)$", message = "Type must be 'student' or 'instructor'")
    private String type; // "student" or "instructor"
}

