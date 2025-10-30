package com.smartattendance.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
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
                recognitionNet = Dnn.readNetFromONNX(
                        resourceUtils.getResourceFilePath("models/facenet.onnx"));
                System.out.println("[INFO] ArcFace model loaded successfully!");
            }
            if (faceDetector == null || faceDetector.empty()) {
                System.out.println("[INFO] Loading Haar Cascade face detector...");
                faceDetector = new CascadeClassifier(
                        resourceUtils.getResourceFilePath("models/haarcascade_frontalface_alt.xml"));
                if (faceDetector.empty()) {
                    throw new IllegalStateException("Failed to load OpenCV face detection model");
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
            System.out.println("[EMBEDDING] Computing embedding from 112x112 face region...");
            float[] embedding = FaceEmbeddingUtils.faceToEmbedding(detectedFace, recognitionNet);
            System.out.println("[EMBEDDING] Successfully computed " + embedding.length + "-dimensional embedding");
            return Optional.of(embedding);
        } finally {
            detectedFace.release();
        }
    }
}