package com.smartattendance.dto.response;

public class FaceDetectionResponseDTO {
    private boolean faceDetected;

    public FaceDetectionResponseDTO(boolean faceDetected) {
        this.faceDetected = faceDetected;
    }

    public boolean isFaceDetected() {
        return faceDetected;
    }

    public void setFaceDetected(boolean faceDetected) {
        this.faceDetected = faceDetected;
    }
}

