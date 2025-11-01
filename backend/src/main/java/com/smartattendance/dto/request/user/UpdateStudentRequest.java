package com.smartattendance.dto.request.user;

import com.smartattendance.util.constants.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for updating a student.
 * Contains validation rules for all required fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentRequest {
    
    @NotBlank(message = ValidationConstants.EMAIL_REQUIRED_MESSAGE)
    @Email(message = ValidationConstants.EMAIL_INVALID_MESSAGE)
    private String email;
    
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
    
    @Size(max = 8, message = "You can upload up to 8 face images")
    private List<String> faceImages = new ArrayList<>();
}

