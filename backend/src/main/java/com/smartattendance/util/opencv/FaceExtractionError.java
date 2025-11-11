package com.smartattendance.util.opencv;

/**
 * Enumerates the possible failure reasons when attempting to extract a face
 * from an image.
 */
public enum FaceExtractionError {
    IMAGE_DECODE_FAILED,
    NO_FACE_DETECTED,
    MULTIPLE_FACES_DETECTED,
    FACE_QUALITY_REJECTED,
    MODEL_ERROR,
    UNKNOWN_ERROR
}

