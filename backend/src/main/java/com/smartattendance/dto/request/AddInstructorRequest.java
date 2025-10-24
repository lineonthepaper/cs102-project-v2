package com.smartattendance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddInstructorRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}

