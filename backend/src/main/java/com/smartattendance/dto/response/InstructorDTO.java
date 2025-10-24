package com.smartattendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private String authId;
    private List<SectionAssignmentDTO> sectionAssignments;
}