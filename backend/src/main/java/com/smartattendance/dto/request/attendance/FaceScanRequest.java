package com.smartattendance.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for face scan submissions coming from the attendance scanner UI.
 * Carries a base64-encoded image captured from the camera stream.
 */
public class FaceScanRequest {

    @NotBlank(message = "Image data is required for face scanning")
    private String imageData;

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}

