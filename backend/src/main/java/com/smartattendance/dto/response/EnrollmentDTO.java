package com.smartattendance.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {
    private Long id;
    private String userId;
    private Long sectionId;
    private Boolean isActive;
    private LocalDateTime enrolledAt;
    private SectionDTO section;
}

