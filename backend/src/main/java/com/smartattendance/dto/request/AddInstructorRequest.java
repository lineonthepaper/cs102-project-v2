package com.smartattendance.dto.request;

import com.smartattendance.util.constants.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for adding a new instructor.
 * Contains validation rules for all required fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddInstructorRequest {
    
    @NotBlank(message = ValidationConstants.EMAIL_REQUIRED_MESSAGE)
    @Email(message = ValidationConstants.EMAIL_INVALID_MESSAGE)
    private String email;
    
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "First name is required")
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, 
          max = ValidationConstants.NAME_MAX_LENGTH, 
          message = "First name must be between 1 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, 
          max = ValidationConstants.NAME_MAX_LENGTH, 
          message = "Last name must be between 1 and 50 characters")
    private String lastName;
}

