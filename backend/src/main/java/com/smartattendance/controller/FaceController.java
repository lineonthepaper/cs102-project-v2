package com.smartattendance.controller;

import com.smartattendance.dto.request.attendance.FaceScanRequest;
import com.smartattendance.dto.response.FaceDetectionResponseDTO;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.util.helper.ResourcePathUtils;
import com.smartattendance.util.opencv.FaceDetectionUtils;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/api/face")
@CrossOrigin(origins = "http://localhost:5173")
public class FaceController {

    private final ResourcePathUtils resourceUtils;
    private CascadeClassifier faceDetector;

    public FaceController(ResourcePathUtils resourceUtils) {
        this.resourceUtils = resourceUtils;
    }

    @PostConstruct
    public void initializeDetector() {
        // Initialize face detector using ResourcePathUtils
        faceDetector = new CascadeClassifier(
                resourceUtils.getResourceFilePath("models/haarcascade_frontalface_alt.xml"));
        if (faceDetector.empty()) {
            throw new RuntimeException("Failed to load Haar cascade classifier");
        }
    }

    @PostMapping("/detect")
    public ResponseEntity<FaceDetectionResponseDTO> detectFace(
            @Valid @RequestBody FaceScanRequest request) {
        
        byte[] imageBytes = decodeBase64(request.getImageData());
        
        // Use FaceDetectionUtils to check if a face exists
        Mat faceMat = FaceDetectionUtils.getFaceFromImageBytes(imageBytes, faceDetector);
        boolean faceDetected = faceMat != null && !faceMat.empty();
        
        // Clean up
        if (faceMat != null) {
            faceMat.release();
        }
        
        return ResponseEntity.ok(new FaceDetectionResponseDTO(faceDetected));
    }

    private byte[] decodeBase64(String imageData) {
        if (imageData == null || imageData.isBlank()) {
            throw new InvalidRequestException("Image data cannot be empty");
        }
        String sanitized = imageData.contains(",") ? imageData.substring(imageData.indexOf(',') + 1) : imageData;
        try {
            return Base64.getDecoder().decode(sanitized);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid base64 image data");
        }
    }
}

