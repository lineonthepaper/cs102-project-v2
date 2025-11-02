package com.smartattendance.util.opencv;
import java.io.File;
import java.util.List;

import org.opencv.core.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;
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

        // --- SCRFD PATH (preferred) ---
        OnnxFaceDetector scrfdDetector = getScrfd();
        if (scrfdDetector != null) {
            System.out.println("[FACE DETECTION] Using SCRFD ONNX detector...");
            OnnxFaceDetector.MultiFaceResult result = scrfdDetector.detectMulti(image);
            
            if (result != null && result.faceCount > 1) {
                System.out.println("[FACE DETECTION] FAILED: Multiple faces detected (" + result.faceCount + ")");
                System.out.println("[FACE DETECTION] ========================================\n");
                return null; // Reject multi-face
            }
            
            if (result != null && result.bestFace != null) {
                OnnxFaceDetector.FaceWithLandmarks det = result.bestFace;
                if (det.bbox != null && det.landmarks != null && det.landmarks.length == 5) {
                    System.out.println("[FACE DETECTION] SCRFD found face with 5 landmarks");
                    
                    // Standard 5-point reference for ArcFace alignment (normalized to 112x112)
                    Point[] std = new Point[] {
                        new Point(38.2946f, 51.6963f),  // right eye
                        new Point(73.5318f, 51.5014f),  // left eye
                        new Point(56.0252f, 71.7366f),  // nose
                        new Point(41.5493f, 92.3655f),  // right mouth
                        new Point(70.7299f, 92.2041f)   // left mouth
                    };
                    
                    // Use 3-point affine (eyes + nose)
                    Point[] srcPtsArr = new Point[] { det.landmarks[0], det.landmarks[1], det.landmarks[2] };
                    Point[] dstPtsArr = new Point[] { std[0], std[1], std[2] };
                    MatOfPoint2f src = new MatOfPoint2f(srcPtsArr);
                    MatOfPoint2f dst = new MatOfPoint2f(dstPtsArr);
                    Mat affine = Imgproc.getAffineTransform(src, dst);
                    
                    if (affine != null && !affine.empty()) {
                        Mat aligned = new Mat();
                        Imgproc.warpAffine(image, aligned, affine, new Size(112,112), 
                                          Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(128,128,128));
                        
                        // More lenient quality checks for SCRFD (already has good alignment)
                        boolean ok = QualityUtils.passesQuality(aligned, 90, 30.0, 15.0, 240.0);
                        if (!ok) {
                            System.out.println("[FACE DETECTION] SCRFD face rejected by quality check");
                            // Don't return null yet, try Haar fallback
                        } else {
                            System.out.println("[FACE DETECTION] SUCCESS: SCRFD with affine alignment");
                            System.out.println("[FACE DETECTION] ========================================\n");
                            return new FaceDetectionResult(aligned, det.bbox, originalWidth, originalHeight);
                        }
                    }
                }
            } else {
                System.out.println("[FACE DETECTION] SCRFD did not find valid face/landmarks");
            }
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

        // Try SCRFD first
        OnnxFaceDetector scrfdDetector = getScrfd();
        if (scrfdDetector != null && scrfdDetector.isLoaded()) {
            System.out.println("[MULTI-FACE DETECTION] Using SCRFD ONNX detector...");
            java.util.List<OnnxFaceDetector.FaceWithLandmarks> allFaces = scrfdDetector.detectAllFaces(image);
            
            if (allFaces != null && !allFaces.isEmpty()) {
                System.out.println("[MULTI-FACE DETECTION] SCRFD detected " + allFaces.size() + " raw face(s)");
                
                // Standard 5-point reference for ArcFace alignment
                Point[] std = new Point[] {
                    new Point(38.2946f, 51.6963f),  // right eye
                    new Point(73.5318f, 51.5014f),  // left eye
                    new Point(56.0252f, 71.7366f),  // nose
                    new Point(41.5493f, 92.3655f),  // right mouth
                    new Point(70.7299f, 92.2041f)   // left mouth
                };
                
                int processedCount = 0;
                for (OnnxFaceDetector.FaceWithLandmarks det : allFaces) {
                    if (det.bbox != null && det.landmarks != null && det.landmarks.length == 5) {
                        // Use 3-point affine (eyes + nose)
                        Point[] srcPtsArr = new Point[] { det.landmarks[0], det.landmarks[1], det.landmarks[2] };
                        Point[] dstPtsArr = new Point[] { std[0], std[1], std[2] };
                        MatOfPoint2f src = new MatOfPoint2f(srcPtsArr);
                        MatOfPoint2f dst = new MatOfPoint2f(dstPtsArr);
                        Mat affine = Imgproc.getAffineTransform(src, dst);
                        
                        if (affine != null && !affine.empty()) {
                            Mat aligned = new Mat();
                            Imgproc.warpAffine(image, aligned, affine, new Size(112,112), 
                                              Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(128,128,128));
                            
                            // Quality checks for SCRFD
                            boolean ok = QualityUtils.passesQuality(aligned, 90, 30.0, 15.0, 240.0);
                            if (ok) {
                                results.add(new FaceDetectionResult(aligned, det.bbox, originalWidth, originalHeight));
                                processedCount++;
                            } else {
                                System.out.println("[MULTI-FACE DETECTION] SCRFD face rejected by quality check");
                                aligned.release();
                            }
                        }
                    }
                }
                
                System.out.println("[MULTI-FACE DETECTION] " + processedCount + " face(s) passed quality checks");
                if (!results.isEmpty()) {
                    System.out.println("[MULTI-FACE DETECTION] Successfully processed " + results.size() + " face(s) with SCRFD");
                    System.out.println("[MULTI-FACE DETECTION] ========================================\n");
                    return results;
                }
            }
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
