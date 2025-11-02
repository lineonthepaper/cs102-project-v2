package com.smartattendance.dto.response.attendance;

/**
 * Information about a single detected face during scanning.
 */
public class FaceDetectionDTO {
    private BoundingBoxDTO boundingBox;
    private RecognizedStudentDTO student;
    private double similarity;

    public FaceDetectionDTO() {
    }

    public FaceDetectionDTO(BoundingBoxDTO boundingBox, RecognizedStudentDTO student, double similarity) {
        this.boundingBox = boundingBox;
        this.student = student;
        this.similarity = similarity;
    }

    public BoundingBoxDTO getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBoxDTO boundingBox) {
        this.boundingBox = boundingBox;
    }

    public RecognizedStudentDTO getStudent() {
        return student;
    }

    public void setStudent(RecognizedStudentDTO student) {
        this.student = student;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}

