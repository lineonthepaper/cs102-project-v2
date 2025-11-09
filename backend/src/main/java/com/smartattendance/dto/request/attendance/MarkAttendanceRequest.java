package com.smartattendance.dto.request.attendance;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkAttendanceRequest {
    private Long sessionId;
    private String userId;
    private String status;
    private String checkinTime;
    private String notes;
    private double confidenceLevel;
    
    @JsonProperty
    private boolean isAutomatic;
}

