package com.smartattendance.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smartattendance.entity.ComparisonResult;
import com.smartattendance.util.helper.ResourcePathUtils;
import com.smartattendance.util.opencv.FaceDetectionResult;
import com.smartattendance.util.opencv.FaceDetectionUtils;
import com.smartattendance.util.opencv.FaceEmbeddingUtils;
import com.smartattendance.util.opencv.FaceExtractionOutcome;
import com.smartattendance.util.opencv.FaceRecognitionUtils;

@Service
public class FaceRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionService.class);

    @Value("${face.similarityThreshold:0.88}")
    private double similarityThreshold;

    @Value("${face.perImageThreshold:0.85}")
    private float perImageThreshold;

    @Value("${face.minHitsRequired:2}")
    private int minHitsRequired;

    @Value("${face.singleImageStrictThreshold:0.92}")
    private float singleImageStrictThreshold;

    @Value("${face.top2Margin:0.03}")
    private float top2Margin;

    private final ResourcePathUtils resourceUtils;
    private final Object modelLock = new Object();

    private volatile CascadeClassifier faceDetector;

    public FaceRecognitionService(ResourcePathUtils resourceUtils) {
        this.resourceUtils = resourceUtils;
    }

    @PostConstruct
    public void initializeModels() {
        ensureModelsLoaded();
    }

    public Optional<FaceMatch> matchFace(byte[] imageBytes, Map<String, List<float[]>> knownEmbeddings) {
        ensureModelsLoaded();

        if (knownEmbeddings == null || knownEmbeddings.isEmpty()) {
            logger.warn("[RECOGNITION] No known embeddings provided");
            return Optional.empty();
        }

        FaceEmbeddingResult embeddingResult = computeEmbeddingDetailed(imageBytes);
        if (!embeddingResult.isSuccess()) {
            logger.warn("[RECOGNITION] {}", embeddingResult.getMessage());
            return Optional.empty();
        }

        float[] liveEmbedding = embeddingResult.getEmbedding();
        logger.debug("[RECOGNITION] Embedding computed: {} dimensions", liveEmbedding.length);

        ComparisonResult result = FaceRecognitionUtils.findBestMatch(
                liveEmbedding,
                knownEmbeddings,
                similarityThreshold,
                perImageThreshold,
                minHitsRequired,
                singleImageStrictThreshold,
                top2Margin);

        if (result == null || !result.isMatch()) {
            return Optional.empty();
        }

        return Optional.of(new FaceMatch(result.getFaceName(), result.getSimilarity()));
    }

    public Optional<ComparisonResult> topCandidate(byte[] imageBytes, Map<String, List<float[]>> knownEmbeddings) {
        ensureModelsLoaded();
        if (knownEmbeddings == null || knownEmbeddings.isEmpty()) {
            return Optional.empty();
        }
        
        FaceEmbeddingResult embeddingResult = computeEmbeddingDetailed(imageBytes);
        if (!embeddingResult.isSuccess()) {
            return Optional.empty();
        }
        
        float[] liveEmbedding = embeddingResult.getEmbedding();
        logger.debug("[RECOGNITION] Embedding computed: {} dimensions", liveEmbedding.length);
        
        ComparisonResult best = FaceRecognitionUtils.findTopCandidate(liveEmbedding, knownEmbeddings);
        return Optional.ofNullable(best);
    }

    private void ensureModelsLoaded() {
        if (faceDetector != null && !faceDetector.empty()) {
            return;
        }

        synchronized (modelLock) {
            if (faceDetector == null || faceDetector.empty()) {
                logger.info("[INFO] Loading Haar Cascade face detector...");
                String detectorPath = resourceUtils.getResourceFilePath("models/haarcascade_frontalface_alt.xml");
                faceDetector = new CascadeClassifier(detectorPath);
                if (faceDetector.empty()) {
                    throw new IllegalStateException(
                        "Failed to load Haar Cascade face detector. " +
                        "File should be at: backend/src/main/resources/models/haarcascade_frontalface_alt.xml");
                }
                logger.info("[INFO] Face detector loaded successfully!");
            }
        }
    }

    public FaceEmbeddingResult computeEmbeddingDetailed(byte[] imageBytes) {
        ensureModelsLoaded();

        FaceExtractionOutcome extraction = FaceDetectionUtils.getFaceFromImageBytesWithBbox(imageBytes, faceDetector);
        if (!extraction.isSuccess()) {
            String message = extraction.getMessage() != null ? extraction.getMessage() : "Face detection failed.";
            logger.warn("[EMBEDDING] {}", message);
            return FaceEmbeddingResult.failure(extraction, message);
        }

        FaceDetectionResult detection = extraction.getDetectionResultOrNull();
        if (detection == null || detection.getFaceMat() == null) {
            String message = "Face detection produced no usable face region.";
            logger.error("[EMBEDDING] {}", message);
            return FaceEmbeddingResult.failure(extraction, message);
        }

        Mat faceMat = detection.getFaceMat();
        try {
            System.out.println("[EMBEDDING] Computing embedding from face region...");
            float[] embedding = FaceEmbeddingUtils.faceToEmbedding(faceMat);
            System.out.println("[EMBEDDING] Successfully computed " + embedding.length + "-dimensional embedding");
            return FaceEmbeddingResult.success(embedding, extraction);
        } finally {
            faceMat.release();
        }
    }

    public Optional<float[]> computeEmbedding(byte[] imageBytes) {
        FaceEmbeddingResult result = computeEmbeddingDetailed(imageBytes);
        return result.isSuccess() ? Optional.of(result.getEmbedding()) : Optional.empty();
    }

    public Optional<EmbeddingWithBbox> computeEmbeddingWithBbox(byte[] imageBytes) {
        FaceEmbeddingResult result = computeEmbeddingDetailed(imageBytes);
        if (!result.isSuccess()) {
            return Optional.empty();
        }

        if (result.getBoundingBox() == null) {
            logger.warn("[EMBEDDING] Missing bounding box information for extracted face");
            return Optional.empty();
        }

        return Optional.of(new EmbeddingWithBbox(
                result.getEmbedding(),
                result.getBoundingBox(),
                result.getOriginalWidth(),
                result.getOriginalHeight()));
    }

    /**
     * Compute embeddings for all detected faces in an image.
     * Returns list of embeddings with their corresponding bounding boxes.
     */
    public List<EmbeddingWithBbox> computeAllEmbeddingsWithBbox(byte[] imageBytes) {
        ensureModelsLoaded();

        List<EmbeddingWithBbox> results = new java.util.ArrayList<>();

        List<FaceExtractionOutcome> detections =
                FaceDetectionUtils.getAllFacesWithBbox(imageBytes, faceDetector);

        if (detections == null || detections.isEmpty()) {
            logger.warn("[MULTI-EMBEDDING] No faces detected - cannot compute embeddings");
            return results;
        }

        logger.info("[MULTI-EMBEDDING] Computing embeddings for {} detected face(s)...", detections.size());

        for (FaceExtractionOutcome outcome : detections) {
            if (outcome.isSuccess()) {
                FaceDetectionResult detection = outcome.getDetectionResultOrNull();
                Mat faceMat = detection.getFaceMat();
                try {
                    float[] embedding = FaceEmbeddingUtils.faceToEmbedding(faceMat);
                    results.add(new EmbeddingWithBbox(
                            embedding,
                            detection.getBoundingBox(),
                            detection.getOriginalWidth(),
                            detection.getOriginalHeight()));
                } catch (Exception ex) {
                    logger.error("[MULTI-EMBEDDING] Failed to compute embedding for detected face", ex);
                } finally {
                    faceMat.release();
                }
            } else {
                logger.error("[MULTI-EMBEDDING] {}", outcome.getMessage());
            }
        }

        logger.info("[MULTI-EMBEDDING] Successfully computed {} embedding(s)", results.size());
        return results;
    }

    @Getter
    public static class FaceEmbeddingResult {
        private final boolean success;
        private final float[] embedding;
        private final FaceExtractionOutcome extractionOutcome;
        private final String message;
        private final org.opencv.core.Rect boundingBox;
        private final int originalWidth;
        private final int originalHeight;

        private FaceEmbeddingResult(boolean success,
                                    float[] embedding,
                                    FaceExtractionOutcome extractionOutcome,
                                    String message,
                                    org.opencv.core.Rect boundingBox,
                                    int originalWidth,
                                    int originalHeight) {
            this.success = success;
            this.embedding = embedding;
            this.extractionOutcome = extractionOutcome;
            this.message = message;
            this.boundingBox = boundingBox;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }

        public static FaceEmbeddingResult success(float[] embedding, FaceExtractionOutcome outcome) {
            FaceDetectionResult detection = outcome != null ? outcome.getDetectionResultOrNull() : null;
            org.opencv.core.Rect bbox = detection != null ? detection.getBoundingBox() : null;
            int width = detection != null ? detection.getOriginalWidth() : 0;
            int height = detection != null ? detection.getOriginalHeight() : 0;
            return new FaceEmbeddingResult(true, embedding, outcome,
                    "Embedding computed successfully", bbox, width, height);
        }

        public static FaceEmbeddingResult failure(FaceExtractionOutcome outcome, String message) {
            return new FaceEmbeddingResult(false, null, outcome, message, null, 0, 0);
        }

        public static FaceEmbeddingResult failure(String message) {
            return failure(null, message);
        }
    }

    public static class EmbeddingWithBbox {
        private final float[] embedding;
        private final org.opencv.core.Rect boundingBox;
        @Getter
        private final int originalWidth;
        @Getter
        private final int originalHeight;

        public EmbeddingWithBbox(float[] embedding, org.opencv.core.Rect boundingBox, 
                                int originalWidth, int originalHeight) {
            this.embedding = embedding;
            this.boundingBox = boundingBox;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }

        public float[] getEmbedding() {
            return embedding;
        }

        public org.opencv.core.Rect getBoundingBox() {
            return boundingBox;
        }
    }
}