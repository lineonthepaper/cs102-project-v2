package com.smartattendance.util.opencv;

import java.util.Optional;

/**
 * Represents the outcome of attempting to extract a single face from an image.
 * When {@code success} is {@code true}, a {@link FaceDetectionResult} will be
 * present. Otherwise the {@code error} and {@code message} fields provide the
 * failure details.
 */
public class FaceExtractionOutcome {

    private final boolean success;
    private final FaceDetectionResult detectionResult;
    private final FaceExtractionError error;
    private final String message;
    private final Integer detectedFacesCount;

    private FaceExtractionOutcome(boolean success,
                                  FaceDetectionResult detectionResult,
                                  FaceExtractionError error,
                                  String message,
                                  Integer detectedFacesCount) {
        this.success = success;
        this.detectionResult = detectionResult;
        this.error = error;
        this.message = message;
        this.detectedFacesCount = detectedFacesCount;
    }

    public static FaceExtractionOutcome success(FaceDetectionResult detectionResult, String message) {
        return new FaceExtractionOutcome(true, detectionResult, null, message, detectionResult != null ? 1 : null);
    }

    public static FaceExtractionOutcome failure(FaceExtractionError error, String message) {
        return failure(error, message, null);
    }

    public static FaceExtractionOutcome failure(FaceExtractionError error,
                                                String message,
                                                Integer detectedFacesCount) {
        return new FaceExtractionOutcome(false, null, error, message, detectedFacesCount);
    }

    public boolean isSuccess() {
        return success;
    }

    public Optional<FaceDetectionResult> getDetectionResult() {
        return Optional.ofNullable(detectionResult);
    }

    public FaceDetectionResult getDetectionResultOrNull() {
        return detectionResult;
    }

    public FaceExtractionError getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Optional<Integer> getDetectedFacesCount() {
        return Optional.ofNullable(detectedFacesCount);
    }
}

