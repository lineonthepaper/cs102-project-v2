package com.smartattendance.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.smartattendance.entity.ComparisonResult;
import com.smartattendance.util.helper.ResourcePathUtils;
import com.smartattendance.util.opencv.FaceDetectionUtils;
import com.smartattendance.util.opencv.FaceEmbeddingUtils;
import com.smartattendance.util.opencv.FaceRecognitionUtils;

@Service
public class FaceRecognitionService {

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

    private volatile Net recognitionNet;
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
            System.out.println("[RECOGNITION] No known embeddings provided");
            return Optional.empty();
        }

        Optional<float[]> embeddingOptional = computeEmbedding(imageBytes);
        if (embeddingOptional.isEmpty()) {
            System.out.println("[RECOGNITION] Failed to compute embedding from image");
            return Optional.empty();
        }

        float[] liveEmbedding = embeddingOptional.get();
        System.out.println("[RECOGNITION] Embedding computed: " + liveEmbedding.length + " dimensions");

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
        
        Optional<float[]> embeddingOptional = computeEmbedding(imageBytes);
        if (embeddingOptional.isEmpty()) {
            return Optional.empty();
        }
        
        float[] liveEmbedding = embeddingOptional.get();
        System.out.println("[RECOGNITION] Embedding computed: " + liveEmbedding.length + " dimensions");
        
        ComparisonResult best = FaceRecognitionUtils.findTopCandidate(liveEmbedding, knownEmbeddings);
        return Optional.ofNullable(best);
    }

    private void ensureModelsLoaded() {
        if (recognitionNet != null && faceDetector != null && !faceDetector.empty()) {
            return;
        }

        synchronized (modelLock) {
            if (recognitionNet == null) {
                System.out.println("[INFO] Loading ArcFace ResNet100 model...");
                String modelPath = resourceUtils.getResourceFilePath("models/facenet.onnx");
                
                // Check if resource loading failed
                java.nio.file.Path path = null;
                boolean pathValid = false;
                try {
                    if (modelPath != null && !modelPath.startsWith("Cannot get resource")) {
                        path = java.nio.file.Paths.get(modelPath);
                        pathValid = java.nio.file.Files.exists(path) && java.nio.file.Files.isRegularFile(path);
                    }
                } catch (Exception e) {
                    pathValid = false;
                }
                
                if (!pathValid) {
                    String errorMessage = "\n" +
                        "═══════════════════════════════════════════════════════════════\n" +
                        "ERROR: Face recognition model not found!\n" +
                        "═══════════════════════════════════════════════════════════════\n\n" +
                        "The ArcFace model (facenet.onnx) is required but not found.\n\n" +
                        "SOLUTION:\n" +
                        "1. Run the setup script to download it automatically:\n" +
                        "   macOS/Linux: ./scripts/setup-models.sh\n" +
                        "   Windows:     scripts\\setup-models.bat\n\n" +
                        "2. Or download manually:\n" +
                        "   URL: https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx\n" +
                        "   Save to: backend/src/main/resources/models/facenet.onnx\n\n" +
                        "3. Verify the file is ~249 MB after download\n" +
                        "═══════════════════════════════════════════════════════════════\n";
                    throw new IllegalStateException(errorMessage);
                }
                
                try {
                    recognitionNet = Dnn.readNetFromONNX(modelPath);
                    if (recognitionNet.empty()) {
                        throw new IllegalStateException("Failed to load ArcFace model - file may be corrupted");
                    }
                    System.out.println("[INFO] ArcFace model loaded successfully!");
                } catch (org.opencv.core.CvException e) {
                    String errorMessage = "\n" +
                        "═══════════════════════════════════════════════════════════════\n" +
                        "ERROR: Failed to load ArcFace model!\n" +
                        "═══════════════════════════════════════════════════════════════\n\n" +
                        "The model file exists but cannot be loaded.\n\n" +
                        "POSSIBLE CAUSES:\n" +
                        "1. Model file is corrupted or incomplete\n" +
                        "2. Model file is the wrong format\n" +
                        "3. OpenCV version mismatch\n\n" +
                        "SOLUTION:\n" +
                        "1. Delete the existing model file:\n" +
                        "   rm backend/src/main/resources/models/facenet.onnx\n\n" +
                        "2. Re-run the setup script:\n" +
                        "   macOS/Linux: ./scripts/setup-models.sh\n" +
                        "   Windows:     scripts\\setup-models.bat\n\n" +
                        "Original error: " + e.getMessage() + "\n" +
                        "═══════════════════════════════════════════════════════════════\n";
                    throw new IllegalStateException(errorMessage, e);
                }
            }
            if (faceDetector == null || faceDetector.empty()) {
                System.out.println("[INFO] Loading Haar Cascade face detector...");
                String detectorPath = resourceUtils.getResourceFilePath("models/haarcascade_frontalface_alt.xml");
                faceDetector = new CascadeClassifier(detectorPath);
                if (faceDetector.empty()) {
                    throw new IllegalStateException(
                        "Failed to load Haar Cascade face detector. " +
                        "File should be at: backend/src/main/resources/models/haarcascade_frontalface_alt.xml");
                }
                System.out.println("[INFO] Face detector loaded successfully!");
            }
        }
    }

    public Optional<float[]> computeEmbedding(byte[] imageBytes) {
        ensureModelsLoaded();

        Mat detectedFace = FaceDetectionUtils.getFaceFromImageBytes(imageBytes, faceDetector);
        if (detectedFace == null) {
            System.out.println("[EMBEDDING] Face detection failed - cannot compute embedding");
            return Optional.empty();
        }

        try {
            System.out.println("[EMBEDDING] Computing embedding from face region...");
            float[] embedding = FaceEmbeddingUtils.faceToEmbedding(detectedFace, recognitionNet);
            System.out.println("[EMBEDDING] Successfully computed " + embedding.length + "-dimensional embedding");
            return Optional.of(embedding);
        } finally {
            detectedFace.release();
        }
    }

    public Optional<EmbeddingWithBbox> computeEmbeddingWithBbox(byte[] imageBytes) {
        ensureModelsLoaded();

        com.smartattendance.util.opencv.FaceDetectionResult result = 
            com.smartattendance.util.opencv.FaceDetectionUtils.getFaceFromImageBytesWithBbox(imageBytes, faceDetector);
        if (result == null) {
            System.out.println("[EMBEDDING] Face detection failed - cannot compute embedding");
            return Optional.empty();
        }

        try {
            System.out.println("[EMBEDDING] Computing embedding from face region...");
            float[] embedding = FaceEmbeddingUtils.faceToEmbedding(result.getFaceMat(), recognitionNet);
            System.out.println("[EMBEDDING] Successfully computed " + embedding.length + "-dimensional embedding");
            return Optional.of(new EmbeddingWithBbox(embedding, result.getBoundingBox(), 
                                                     result.getOriginalWidth(), result.getOriginalHeight()));
        } finally {
            result.getFaceMat().release();
        }
    }

    /**
     * Compute embeddings for all detected faces in an image.
     * Returns list of embeddings with their corresponding bounding boxes.
     */
    public List<EmbeddingWithBbox> computeAllEmbeddingsWithBbox(byte[] imageBytes) {
        ensureModelsLoaded();

        List<EmbeddingWithBbox> results = new java.util.ArrayList<>();
        
        List<com.smartattendance.util.opencv.FaceDetectionResult> detections = 
            com.smartattendance.util.opencv.FaceDetectionUtils.getAllFacesWithBbox(imageBytes, faceDetector);
        
        if (detections == null || detections.isEmpty()) {
            System.out.println("[MULTI-EMBEDDING] No faces detected - cannot compute embeddings");
            return results;
        }

        System.out.println("[MULTI-EMBEDDING] Computing embeddings for " + detections.size() + " detected face(s)...");
        
        for (com.smartattendance.util.opencv.FaceDetectionResult detection : detections) {
            try {
                float[] embedding = FaceEmbeddingUtils.faceToEmbedding(detection.getFaceMat(), recognitionNet);
                results.add(new EmbeddingWithBbox(embedding, detection.getBoundingBox(), 
                                                 detection.getOriginalWidth(), detection.getOriginalHeight()));
            } finally {
                detection.getFaceMat().release();
            }
        }
        
        System.out.println("[MULTI-EMBEDDING] Successfully computed " + results.size() + " embeddings");
        return results;
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