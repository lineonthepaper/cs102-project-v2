package com.smartattendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TADTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isStudent;
    private Boolean isInstructor;
    private Boolean isTA;
    private Boolean enabled;
    private String authId;
    private List<TAAssignmentDTO> taAssignments;
}