package com.smartattendance.dto.request;

import com.smartattendance.util.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for creating a new student.
 * Contains validation rules for all required fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentRequest {
    
    @NotBlank(message = ValidationConstants.EMAIL_REQUIRED_MESSAGE)
    @Email(message = ValidationConstants.EMAIL_INVALID_MESSAGE)
    private String email;
    
    @NotBlank(message = "First name is required")
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, 
          max = ValidationConstants.NAME_MAX_LENGTH, 
          message = "First name " + ValidationConstants.NAME_MIN_MESSAGE)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, 
          max = ValidationConstants.NAME_MAX_LENGTH, 
          message = "Last name " + ValidationConstants.NAME_MIN_MESSAGE)
    private String lastName;
}

