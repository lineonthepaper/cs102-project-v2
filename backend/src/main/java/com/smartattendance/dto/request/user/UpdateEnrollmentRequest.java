package com.smartattendance.dto.request.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEnrollmentRequest {
    private List<Long> sectionIds;
}

