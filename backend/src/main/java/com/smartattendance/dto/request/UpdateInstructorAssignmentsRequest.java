package com.smartattendance.dto.request;

import java.util.List;

public class UpdateInstructorAssignmentsRequest {
    private List<Long> sectionIds;

    public UpdateInstructorAssignmentsRequest() {
    }

    public UpdateInstructorAssignmentsRequest(List<Long> sectionIds) {
        this.sectionIds = sectionIds;
    }

    public List<Long> getSectionIds() {
        return sectionIds;
    }

    public void setSectionIds(List<Long> sectionIds) {
        this.sectionIds = sectionIds;
    }
}

