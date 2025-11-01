package com.smartattendance.util.opencv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Result from face detection containing both the detected face and its bounding box.
 */
public class FaceDetectionResult {
    private final Mat faceMat;
    private final Rect boundingBox;
    private final int originalWidth;
    private final int originalHeight;

    public FaceDetectionResult(Mat faceMat, Rect boundingBox, int originalWidth, int originalHeight) {
        this.faceMat = faceMat;
        this.boundingBox = boundingBox;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
    }

    public Mat getFaceMat() {
        return faceMat;
    }

    public Rect getBoundingBox() {
        return boundingBox;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }
}

