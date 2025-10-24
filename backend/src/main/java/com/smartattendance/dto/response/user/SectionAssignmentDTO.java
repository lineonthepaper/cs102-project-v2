package com.smartattendance.dto.response.user;

import com.smartattendance.dto.response.course.SectionDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionAssignmentDTO {
    private Long id;
    private String userId;
    private Long sectionId;
    private String role;
    private Boolean isActive;
    private SectionDTO section;
}