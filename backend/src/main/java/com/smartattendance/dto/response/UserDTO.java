package com.smartattendance.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isStudent;
    private Boolean isInstructor;
    private Boolean isTA;
    private Boolean enabled;
    private String authId;
}

