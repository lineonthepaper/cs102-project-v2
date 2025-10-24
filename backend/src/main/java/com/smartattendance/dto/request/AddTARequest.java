package com.smartattendance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTARequest {
    private String email;
    private String password;
    private String type; // "student" or "instructor"
}

