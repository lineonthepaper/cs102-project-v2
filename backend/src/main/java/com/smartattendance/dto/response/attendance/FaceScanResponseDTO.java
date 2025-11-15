package com.smartattendance.dto.response.attendance;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

@Data
public class FaceScanResponseDTO {

    private boolean matched;
    private boolean alreadyMarked;
    private double similarity;
    private RecognizedStudentDTO student;
    private String recommendedStatus;
    private OffsetDateTime recommendedCheckInTime;
    private String message;
    private BoundingBoxDTO boundingBox;
    private List<FaceDetectionDTO> allDetections;
    private List<RecognizedStudentDTO> allStudents;
    private double margin;

    public FaceScanResponseDTO() {}
}
