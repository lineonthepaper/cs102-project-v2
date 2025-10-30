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

import com.smartattendance.entity.ComparisonResult;
import com.smartattendance.util.helper.ResourcePathUtils;
import com.smartattendance.util.opencv.FaceDetectionUtils;
import com.smartattendance.util.opencv.FaceEmbeddingUtils;
import com.smartattendance.util.opencv.FaceRecognitionUtils;

@Service
public class FaceRecognitionService {

    // ArcFace threshold is lower than SFace because it produces different embedding spaces
    // Typical ArcFace threshold: 0.4-0.6 (lower = stricter matching)
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.50;

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
            return Optional.empty();
        }

        System.out.println("[DEBUG] Computing embedding for live camera frame...");
        Optional<float[]> embeddingOptional = computeEmbedding(imageBytes);
        if (embeddingOptional.isEmpty()) {
            System.out.println("[DEBUG] Failed to compute embedding for live frame");
            return Optional.empty();
        }

        float[] liveEmbedding = embeddingOptional.get();
        System.out.println("[DEBUG] Live embedding computed: " + liveEmbedding.length + " dimensions, " +
                         "first 3 values: [" + liveEmbedding[0] + ", " + liveEmbedding[1] + ", " + liveEmbedding[2] + "]");

        ComparisonResult result = FaceRecognitionUtils.findBestMatch(liveEmbedding, knownEmbeddings,
                DEFAULT_SIMILARITY_THRESHOLD);

        if (result == null || !result.isMatch()) {
            return Optional.empty();
        }

        return Optional.of(new FaceMatch(result.getFaceName(), result.getSimilarity()));
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
            return Optional.empty();
        }

        try {
            return Optional.of(FaceEmbeddingUtils.faceToEmbedding(detectedFace, recognitionNet));
        } finally {
            detectedFace.release();
        }
    }
}