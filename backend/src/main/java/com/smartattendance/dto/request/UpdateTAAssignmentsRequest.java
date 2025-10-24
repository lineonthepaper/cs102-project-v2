package com.smartattendance.dto.request;

import java.util.List;

public class UpdateTAAssignmentsRequest {
    private List<Long> sectionIds;

    public UpdateTAAssignmentsRequest() {
    }

    public UpdateTAAssignmentsRequest(List<Long> sectionIds) {
        this.sectionIds = sectionIds;
    }

    public List<Long> getSectionIds() {
        return sectionIds;
    }

    public void setSectionIds(List<Long> sectionIds) {
        this.sectionIds = sectionIds;
    }
}

