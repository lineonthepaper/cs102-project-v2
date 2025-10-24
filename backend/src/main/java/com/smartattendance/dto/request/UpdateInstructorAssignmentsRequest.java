package com.smartattendance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInstructorAssignmentsRequest {
    private List<Long> sectionIds;
}

