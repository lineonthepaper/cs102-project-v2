package com.smartattendance.dto.response.attendance;

import lombok.Data;

@Data
public class FaceDetectionDTO {

    private BoundingBoxDTO boundingBox;
    private RecognizedStudentDTO student;
    private double similarity;

    public FaceDetectionDTO() {}

    public FaceDetectionDTO(BoundingBoxDTO boundingBox,
                            RecognizedStudentDTO student,
                            double similarity) {
        this.boundingBox = boundingBox;
        this.student = student;
        this.similarity = similarity;
    }
}

