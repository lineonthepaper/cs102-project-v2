package com.smartattendance.dto.response.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a single detected face during scanning.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceDetectionDTO {

    private BoundingBoxDTO boundingBox;
    private RecognizedStudentDTO student;
    private double similarity;
    
}

