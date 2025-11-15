package com.smartattendance.dto.response.attendance;

public class FaceDetectionDTO {

    private BoundingBoxDTO boundingBox;
    private RecognizedStudentDTO student;
    private double similarity;
    private double rawSimilarity;

    public FaceDetectionDTO() {}

    public FaceDetectionDTO(BoundingBoxDTO boundingBox,
                            RecognizedStudentDTO student,
                            double similarity,
                            double rawSimilarity) {
        this.boundingBox = boundingBox;
        this.student = student;
        this.similarity = similarity;
        this.rawSimilarity = rawSimilarity;
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

    public double getRawSimilarity() {
        return rawSimilarity;
    }

    public void setRawSimilarity(double rawSimilarity) {
        this.rawSimilarity = rawSimilarity;
    }
}

