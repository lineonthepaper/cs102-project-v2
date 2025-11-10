package com.smartattendance.util.opencv;
import java.io.File;
import java.util.List;

import org.opencv.core.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;

import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.DetectedObjects.DetectedObject;
import ai.djl.modality.cv.output.Point;
import ai.djl.modality.cv.output.BoundingBox;

// Note: Landmark alignment requires OpenCV contrib (org.opencv.face). Not available in current build.

public class FaceDetectionUtils {

    // Only one public detector. Prefer SCRFD ONNX, fallback to Haar cascade.
    private static volatile OnnxFaceDetector scrfd = null;
    private static final Object scrfdLock = new Object();
    
    /**
     * Initialize SCRFD detector using ResourcePathUtils.
     * Should be called once at application startup from a service with ResourcePathUtils.
     */
    public static void initializeScrfd(String modelPath) {
        if (scrfd == null) {
            synchronized (scrfdLock) {
                if (scrfd == null) {
                    if (modelPath != null && !modelPath.isEmpty()) {
                        File f = new File(modelPath);
                        if (f.exists()) {
                            System.out.println("[SCRFD INIT] Loading SCRFD model from: " + modelPath);
                            OnnxFaceDetector d = new OnnxFaceDetector(modelPath);
                            if (d.isLoaded()) {
                                scrfd = d;
                                System.out.println("[SCRFD INIT] Successfully loaded SCRFD detector");
                            } else {
                                System.out.println("[SCRFD INIT] Failed to load SCRFD detector (model not loaded)");
                            }
                        } else {
                            System.out.println("[SCRFD INIT] SCRFD model file not found at: " + modelPath);
                        }
                    } else {
                        System.out.println("[SCRFD INIT] No SCRFD model path provided");
                    }
                }
            }
        }
    }
    
    private static OnnxFaceDetector getScrfd() {
        return scrfd;
    }

    public static Mat getFaceFromImageBytes(byte[] imageBytes, CascadeClassifier faceDetector) {
        FaceDetectionResult result = getFaceFromImageBytesWithBbox(imageBytes, faceDetector);
        return result != null ? result.getFaceMat() : null;
    }

    public static FaceDetectionResult getFaceFromImageBytesWithBbox(byte[] imageBytes, CascadeClassifier faceDetector) {
        System.out.println("\n[FACE DETECTION] ========================================");
        
        // Convert bytes to Mat
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        if (image.empty() || image.width() == 0 || image.height() == 0) {
            System.out.println("[FACE DETECTION] Failed: Could not decode image");
            return null;
        }
        System.out.println("[FACE DETECTION] Image decoded: " + image.width() + "x" + image.height());
        int originalWidth = image.width();
        int originalHeight = image.height();

        // light face detection!!!
        try {
            DetectedObjects detectedObjects = LightFaceDetection.predict(image);
            int numObjects = detectedObjects.getNumberOfObjects();
    
            System.out.println("[FACE DETECTION] LightFace detected " + numObjects + " face(s)");
    
            if (numObjects == 1) {
                DetectedObject face = detectedObjects.item(0);
                BoundingBox bbox = face.getBoundingBox();
                Point topLeft = bbox.getPoint();
                Rect faceRect = new Rect((int)topLeft.getX(), (int)topLeft.getY(), originalWidth, originalHeight);

                Mat resized = new Mat();
                int interp = (originalWidth >= 112 || originalHeight >= 112) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;
                Imgproc.resize(image, resized, new Size(112,112), 0, 0, interp);

                System.out.println("[FACE DETECTION] ========================================\n");
                return new FaceDetectionResult(resized, faceRect, originalWidth, originalHeight);
            } else if (numObjects == 0) {
                    System.out.println("[FACE DETECTION] FAILED: No face detected");
                    System.out.println("[FACE DETECTION] ========================================\n");
                    return null;
            } else {
                System.out.println("[FACE DETECTION] FAILED: Multiple faces detected (" + numObjects + ")");
                return null;
            }
    
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // --- HAAR FALLBACK ---
        System.out.println("[FACE DETECTION] Falling back to Haar Cascade detector...");
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        
        // Apply CLAHE for better contrast
        try {
            CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
            Mat grayClahe = new Mat();
            clahe.apply(gray, grayClahe);
            gray.release();
            gray = grayClahe;
        } catch (Exception e) {
            Imgproc.equalizeHist(gray, gray);
        }

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(
            gray, faceDetections, 1.1, 3, 0, new Size(30, 30), new Size()
        );
        Rect[] faces = faceDetections.toArray();
        
        System.out.println("[FACE DETECTION] Haar detected " + faces.length + " face(s)");
        if (faces.length == 0) {
            System.out.println("[FACE DETECTION] FAILED: No face detected");
            System.out.println("[FACE DETECTION] ========================================\n");
            return null;
        }
        
        if (faces.length > 1) {
            System.out.println("[FACE DETECTION] FAILED: Multiple faces detected (" + faces.length + ")");
            System.out.println("[FACE DETECTION] ========================================\n");
            return null; // Reject multi-face
        }
        
        // Use the single detected face
        Rect faceRect = faces[0];
        
        System.out.println("[FACE DETECTION] Selected largest face: " + 
                         faceRect.width + "x" + faceRect.height + " at (" + faceRect.x + "," + faceRect.y + ")");
        
        // Expand to square with margin
        int centerX = faceRect.x + faceRect.width / 2;
        int centerY = faceRect.y + faceRect.height / 2;
        int maxSide = Math.max(faceRect.width, faceRect.height);
        int sideWithMargin = (int) Math.round(maxSide * 1.2);
        
        int x = centerX - sideWithMargin / 2;
        int y = centerY - sideWithMargin / 2;
        x = Math.max(0, Math.min(x, image.width() - 1));
        y = Math.max(0, Math.min(y, image.height() - 1));
        int w = Math.min(sideWithMargin, image.width() - x);
        int h = Math.min(sideWithMargin, image.height() - y);
        int side = Math.min(w, h);
        
        Rect squareRoi = new Rect(x, y, side, side);
        Mat face = new Mat(image, squareRoi);
        
        // More lenient quality checks for Haar (less precise alignment)
        boolean ok = QualityUtils.passesQuality(face, 80, 25.0, 15.0, 240.0);
        if (!ok) {
            System.out.println("[FACE DETECTION] FAILED: Face rejected by quality check");
            System.out.println("[FACE DETECTION] ========================================\n");
            return null;
        }
        
        Mat resized = new Mat();
        int interp = (face.width() >= 112 || face.height() >= 112) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;
        Imgproc.resize(face, resized, new Size(112,112), 0, 0, interp);
        
        System.out.println("[FACE DETECTION] SUCCESS: Haar with crop+resize to 112x112");
        System.out.println("[FACE DETECTION] ========================================\n");
        return new FaceDetectionResult(resized, faceRect, originalWidth, originalHeight);
    }

    /**
     * Detect all faces in an image and return aligned face regions for each.
     * This is the multi-face version for parallel recognition processing.
     */
    public static List<FaceDetectionResult> getAllFacesWithBbox(byte[] imageBytes, CascadeClassifier faceDetector) {
        List<FaceDetectionResult> results = new java.util.ArrayList<>();
        
        System.out.println("\n[MULTI-FACE DETECTION] ========================================");
        
        // Convert bytes to Mat
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        if (image.empty() || image.width() == 0 || image.height() == 0) {
            System.out.println("[MULTI-FACE DETECTION] Failed: Could not decode image");
            return results;
        }
        System.out.println("[MULTI-FACE DETECTION] Image decoded: " + image.width() + "x" + image.height());
        int originalWidth = image.width();
        int originalHeight = image.height();

        // retina face detection!!! (multi)
        try {
            DetectedObjects detectedObjects = LightFaceDetection.predict(image);
            int numObjects = detectedObjects.getNumberOfObjects();
    
            System.out.println("[FACE DETECTION] LightFace detected " + numObjects + " face(s)");
            
            for (int i = 0; i < numObjects; i++) {
                DetectedObject face = detectedObjects.item(0);
                BoundingBox bbox = face.getBoundingBox();
                Point topLeft = bbox.getPoint();
                Rect faceRect = new Rect((int)topLeft.getX(), (int)topLeft.getY(), originalWidth, originalHeight);

                Mat resized = new Mat();
                int interp = (originalWidth >= 112 || originalHeight >= 112) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;
                Imgproc.resize(image, resized, new Size(112,112), 0, 0, interp);

                System.out.println("[FACE DETECTION] ========================================\n");
                results.add(new FaceDetectionResult(resized, faceRect, originalWidth, originalHeight));
            }
            return results;
    
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }

        // Fallback to Haar Cascade
        System.out.println("[MULTI-FACE DETECTION] Falling back to Haar Cascade detector...");
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        
        // Apply CLAHE for better contrast
        try {
            CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
            Mat grayClahe = new Mat();
            clahe.apply(gray, grayClahe);
            gray.release();
            gray = grayClahe;
        } catch (Exception e) {
            Imgproc.equalizeHist(gray, gray);
        }

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(
            gray, faceDetections, 1.1, 3, 0, new Size(30, 30), new Size()
        );
        Rect[] faces = faceDetections.toArray();
        
        System.out.println("[MULTI-FACE DETECTION] Haar detected " + faces.length + " face(s)");
        
        for (Rect faceRect : faces) {
            // Expand to square with margin
            int centerX = faceRect.x + faceRect.width / 2;
            int centerY = faceRect.y + faceRect.height / 2;
            int maxSide = Math.max(faceRect.width, faceRect.height);
            int sideWithMargin = (int) Math.round(maxSide * 1.2);
            
            int x = centerX - sideWithMargin / 2;
            int y = centerY - sideWithMargin / 2;
            x = Math.max(0, Math.min(x, image.width() - 1));
            y = Math.max(0, Math.min(y, image.height() - 1));
            int w = Math.min(sideWithMargin, image.width() - x);
            int h = Math.min(sideWithMargin, image.height() - y);
            int side = Math.min(w, h);
            
            Rect squareRoi = new Rect(x, y, side, side);
            Mat face = new Mat(image, squareRoi);
            
            // Quality checks for Haar
            boolean ok = QualityUtils.passesQuality(face, 80, 25.0, 15.0, 240.0);
            if (ok) {
                Mat resized = new Mat();
                int interp = (face.width() >= 112 || face.height() >= 112) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;
                Imgproc.resize(face, resized, new Size(112,112), 0, 0, interp);
                results.add(new FaceDetectionResult(resized, faceRect, originalWidth, originalHeight));
            }
            face.release();
        }
        
        System.out.println("[MULTI-FACE DETECTION] Successfully processed " + results.size() + " face(s)");
        System.out.println("[MULTI-FACE DETECTION] ========================================\n");
        return results;
    }
}
