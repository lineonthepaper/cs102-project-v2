package com.smartattendance.dto.response.attendance;

import java.time.OffsetDateTime;
import java.util.List;

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
    private double rawSimilarity;
    private double margin;

    public FaceScanResponseDTO() {}

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public boolean isAlreadyMarked() {
        return alreadyMarked;
    }

    public void setAlreadyMarked(boolean alreadyMarked) {
        this.alreadyMarked = alreadyMarked;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public RecognizedStudentDTO getStudent() {
        return student;
    }

    public void setStudent(RecognizedStudentDTO student) {
        this.student = student;
    }

    public String getRecommendedStatus() {
        return recommendedStatus;
    }

    public void setRecommendedStatus(String recommendedStatus) {
        this.recommendedStatus = recommendedStatus;
    }

    public OffsetDateTime getRecommendedCheckInTime() {
        return recommendedCheckInTime;
    }

    public void setRecommendedCheckInTime(OffsetDateTime recommendedCheckInTime) {
        this.recommendedCheckInTime = recommendedCheckInTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BoundingBoxDTO getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBoxDTO boundingBox) {
        this.boundingBox = boundingBox;
    }

    public List<FaceDetectionDTO> getAllDetections() {
        return allDetections;
    }

    public void setAllDetections(List<FaceDetectionDTO> allDetections) {
        this.allDetections = allDetections;
    }

    public List<RecognizedStudentDTO> getAllStudents() {
        return allStudents;
    }

    public void setAllStudents(List<RecognizedStudentDTO> allStudents) {
        this.allStudents = allStudents;
    }

    public double getRawSimilarity() {
        return rawSimilarity;
    }

    public void setRawSimilarity(double rawSimilarity) {
        this.rawSimilarity = rawSimilarity;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }
}
