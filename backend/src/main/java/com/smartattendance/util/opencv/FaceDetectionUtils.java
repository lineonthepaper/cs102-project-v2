package com.smartattendance.util.opencv;
import java.io.File;

import org.opencv.core.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;
// Note: Landmark alignment requires OpenCV contrib (org.opencv.face). Not available in current build.

public class FaceDetectionUtils {

    // Only one public detector. Prefer SCRFD ONNX, fallback to Haar cascade.
    private static final String SCRFD_MODEL_PATH = "backend/src/main/resources/models/scrfd_2.5g_kps.onnx";
    private static final OnnxFaceDetector scrfd = loadScrfd();
    private static OnnxFaceDetector loadScrfd() {
        File f = new File(SCRFD_MODEL_PATH);
        if (f.exists()) {
            OnnxFaceDetector d = new OnnxFaceDetector(SCRFD_MODEL_PATH);
            if (d.isLoaded()) return d;
        }
        return null;
    }

    public static Mat getFaceFromImageBytes(byte[] imageBytes, CascadeClassifier faceDetector) {
        System.out.println("\n[FACE DETECTION] ========================================");
        
        // Convert bytes to Mat
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        if (image.empty() || image.width() == 0 || image.height() == 0) {
            System.out.println("[FACE DETECTION] Failed: Could not decode image");
            return null;
        }
        System.out.println("[FACE DETECTION] Image decoded: " + image.width() + "x" + image.height());

        // --- SCRFD PATH (preferred) ---
        if (scrfd != null) {
            System.out.println("[FACE DETECTION] Using SCRFD ONNX detector...");
            OnnxFaceDetector.FaceWithLandmarks det = scrfd.detect(image);
            
            if (det != null && det.bbox != null && det.landmarks != null && det.landmarks.length == 5) {
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
                        return aligned;
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
        
        // Use largest face
        Rect faceRect = faces[0];
        for (Rect r : faces) {
            if (r.width * r.height > faceRect.width * faceRect.height) {
                faceRect = r;
            }
        }
        
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
        return resized;
    }
}
