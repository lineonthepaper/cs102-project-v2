package com.smartattendance.dto.response.attendance;

import java.time.OffsetDateTime;

/**
 * Response model returned after processing a face scan.
 */
public class FaceScanResponseDTO {

    private boolean matched;
    private boolean alreadyMarked;
    private double similarity;
    private RecognizedStudentDTO student;
    private String recommendedStatus;
    private OffsetDateTime recommendedCheckInTime;
    private String message;

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
}

