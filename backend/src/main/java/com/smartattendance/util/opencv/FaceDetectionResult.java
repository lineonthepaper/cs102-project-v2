package com.smartattendance.util.opencv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result from face detection containing both the detected face and its bounding box.
 */
@Getter
@AllArgsConstructor
public class FaceDetectionResult {

    private final Mat faceMat;
    private final Rect boundingBox;
    private final int originalWidth;
    private final int originalHeight;

}

