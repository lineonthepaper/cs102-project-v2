package com.smartattendance.util.opencv;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.CLAHE;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.DetectedObjects.DetectedObject;
import ai.djl.modality.cv.output.Point;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.Rectangle;

// Note: Landmark alignment requires OpenCV contrib (org.opencv.face). Not available in current build.
public final class FaceDetectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(FaceDetectionUtils.class);

    private FaceDetectionUtils() {
        // Utility class
    }

    public static Mat getFaceFromImageBytes(byte[] imageBytes, CascadeClassifier faceDetector) {
        FaceExtractionOutcome outcome = getFaceFromImageBytesWithBbox(imageBytes, faceDetector);
        if (outcome.isSuccess()) {
            FaceDetectionResult result = outcome.getDetectionResultOrNull();
            if (result != null) {
                return result.getFaceMat();
            }
        }
        return null;
    }

    public static FaceExtractionOutcome getFaceFromImageBytesWithBbox(byte[] imageBytes, CascadeClassifier faceDetector) {
        logger.info("[FACE DETECTION] ========================================");

        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        if (image.empty() || image.width() == 0 || image.height() == 0) {
            logger.warn("[FACE DETECTION] Failed: Could not decode image");
            logger.info("[FACE DETECTION] ========================================");
            return FaceExtractionOutcome.failure(
                    FaceExtractionError.IMAGE_DECODE_FAILED,
                    "Unable to decode the uploaded image.");
        }

        logger.debug("[FACE DETECTION] Image decoded: {}x{}", image.width(), image.height());

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
                Rectangle rectangle = bbox.getBounds();
                Rect faceRect = new Rect(
                    new org.opencv.core.Point(rectangle.getX() * originalWidth, rectangle.getY() * originalHeight),
                    new org.opencv.core.Point(rectangle.getX() * originalWidth + rectangle.getWidth() * originalWidth, rectangle.getY() * originalHeight + rectangle.getHeight() * originalHeight)
                );
                
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
                
                // System.out.println("faceRect.x: " + faceRect.x + ", faceRect.y: " + faceRect.y + ", faceRect.width: " + faceRect.width + ", faceRect.height: " + faceRect.height);

                // System.out.println("rectangle x: " + rectangle.getX() + ", rectangle y: " + rectangle.getY() + ", rectangle width: " + rectangle.getWidth() + ", rectangle height: " + rectangle.getWidth());

                // System.out.println("x: " + x + ", y: " + y + ", side: " + side);
                
                Rect squareRoi = new Rect(x, y, side, side);
                Mat croppedFace = new Mat(image, squareRoi);

                Mat resized = new Mat();
                int interp = (originalWidth >= 112 || originalHeight >= 112) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;
                Imgproc.resize(croppedFace, resized, new Size(112,112), 0, 0, interp);

                System.out.println("[FACE DETECTION] ========================================\n");
                return FaceExtractionOutcome.success(new FaceDetectionResult(resized, faceRect, originalWidth, originalHeight), "Face extracted successfully.");

            } else if (numObjects == 0) {
                    System.out.println("[FACE DETECTION] FAILED: No face detected");
                    System.out.println("[FACE DETECTION] ========================================\n");
                    return FaceExtractionOutcome.failure(
                    FaceExtractionError.NO_FACE_DETECTED,
                    "No face was detected in the provided image.",
                    0);
            } else {
                System.out.println("[FACE DETECTION] FAILED: Multiple faces detected (" + numObjects + ")");
                return FaceExtractionOutcome.failure(
                    FaceExtractionError.MULTIPLE_FACES_DETECTED,
                    "Multiple faces detected. Please upload an image with only one face.",
                    numObjects);
            }
    
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        // --- HAAR FALLBACK ---
        System.out.println("[FACE DETECTION] Falling back to Haar Cascade detector...");
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

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
                gray, faceDetections, 1.1, 3, 0, new Size(30, 30), new Size());
        Rect[] faces = faceDetections.toArray();

        logger.debug("[FACE DETECTION] Haar detected {} face(s)", faces.length);

        if (faces.length == 0) {
            logger.warn("[FACE DETECTION] FAILED: No face detected");
            logger.info("[FACE DETECTION] ========================================");
            return FaceExtractionOutcome.failure(
                    FaceExtractionError.NO_FACE_DETECTED,
                    "No face was detected in the provided image.",
                    0);
        }

        if (faces.length > 1) {
            logger.warn("[FACE DETECTION] FAILED: Multiple faces detected ({})", faces.length);
            logger.info("[FACE DETECTION] ========================================");
            return FaceExtractionOutcome.failure(
                    FaceExtractionError.MULTIPLE_FACES_DETECTED,
                    "Multiple faces detected. Please upload an image with only one face.",
                    faces.length);
        }

        Rect faceRect = faces[0];
        logger.debug("[FACE DETECTION] Selected face: {}x{} at ({},{})",
                faceRect.width, faceRect.height, faceRect.x, faceRect.y);

        int centerX = faceRect.x + faceRect.width / 2;
        int centerY = faceRect.y + faceRect.height / 2;
        int maxSide = Math.max(faceRect.width, faceRect.height);
        int sideWithMargin = (int) Math.round(maxSide * 1.2);

        int x = Math.max(0, Math.min(centerX - sideWithMargin / 2, image.width() - 1));
        int y = Math.max(0, Math.min(centerY - sideWithMargin / 2, image.height() - 1));
        int w = Math.min(sideWithMargin, image.width() - x);
        int h = Math.min(sideWithMargin, image.height() - y);
        int side = Math.min(w, h);

        Rect squareRoi = new Rect(x, y, side, side);
        Mat face = new Mat(image, squareRoi);
        try {
            boolean passedQuality = QualityUtils.passesQuality(face, 80, 25.0, 15.0, 240.0);
            if (!passedQuality) {
                logger.warn("[FACE DETECTION] FAILED: Face rejected by quality check");
                logger.info("[FACE DETECTION] ========================================");
                return FaceExtractionOutcome.failure(
                        FaceExtractionError.FACE_QUALITY_REJECTED,
                        "Face failed quality checks (lighting, focus, or occlusion).");
            }

            Mat resized = new Mat();
            int interpolation = (face.width() >= 112 || face.height() >= 112)
                    ? Imgproc.INTER_AREA
                    : Imgproc.INTER_CUBIC;
            Imgproc.resize(face, resized, new Size(112, 112), 0, 0, interpolation);

            logger.info("[FACE DETECTION] SUCCESS: Haar with crop+resize to 112x112");
            logger.info("[FACE DETECTION] ========================================");
            return FaceExtractionOutcome.success(
                    new FaceDetectionResult(resized, faceRect, originalWidth, originalHeight),
                    "Face extracted successfully.");
        } finally {
            face.release();
        }
    }

    public static List<FaceExtractionOutcome> getAllFacesWithBbox(byte[] imageBytes, CascadeClassifier faceDetector) {
        List<FaceExtractionOutcome> results = new ArrayList<>();

        logger.info("[MULTI-FACE DETECTION] ========================================");

        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        if (image.empty() || image.width() == 0 || image.height() == 0) {
            logger.warn("[MULTI-FACE DETECTION] Failed: Could not decode image");
            results.add(FaceExtractionOutcome.failure(
                    FaceExtractionError.IMAGE_DECODE_FAILED,
                    "Unable to decode the uploaded image."));
            return results;
        }

        logger.debug("[MULTI-FACE DETECTION] Image decoded: {}x{}", image.width(), image.height());

        int originalWidth = image.width();
        int originalHeight = image.height();

        // light face detection!!! (multi)
        try {
            DetectedObjects detectedObjects = LightFaceDetection.predict(image);
            int numObjects = detectedObjects.getNumberOfObjects();
    
            System.out.println("[FACE DETECTION] LightFace detected " + numObjects + " face(s)");
            
            for (int i = 0; i < numObjects; i++) {
                DetectedObject face = detectedObjects.item(i);
                BoundingBox bbox = face.getBoundingBox();
                Rectangle rectangle = bbox.getBounds();
                Rect faceRect = new Rect(
                    new org.opencv.core.Point(rectangle.getX() * originalWidth, rectangle.getY() * originalHeight),
                    new org.opencv.core.Point(rectangle.getX() * originalWidth + rectangle.getWidth() * originalWidth, rectangle.getY() * originalHeight + rectangle.getHeight() * originalHeight)
                );
                
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

                // System.out.println("x: " + x + ", y: " + y + ", side: " + side);
                
                Rect squareRoi = new Rect(x, y, side, side);
                Mat croppedFace = new Mat(image, squareRoi);

                Mat resized = new Mat();
                int interp = (originalWidth >= 112 || originalHeight >= 112) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;
                Imgproc.resize(croppedFace, resized, new Size(112,112), 0, 0, interp);

                System.out.println("[FACE DETECTION] ========================================\n");
                results.add(FaceExtractionOutcome.success(new FaceDetectionResult(resized, faceRect, originalWidth, originalHeight), "Face extracted successfully."));
            }

            logger.info("[MULTI-FACE DETECTION] Successfully processed {} face(s)", results.size());
            logger.info("[MULTI-FACE DETECTION] ========================================");

            return results;
    
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }

        // Fallback to Haar Cascade
        System.out.println("[MULTI-FACE DETECTION] Falling back to Haar Cascade detector...");
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

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
                gray, faceDetections, 1.1, 3, 0, new Size(30, 30), new Size());
        Rect[] faces = faceDetections.toArray();

        logger.debug("[MULTI-FACE DETECTION] Haar detected {} face(s)", faces.length);

        for (Rect faceRect : faces) {
            int centerX = faceRect.x + faceRect.width / 2;
            int centerY = faceRect.y + faceRect.height / 2;
            int maxSide = Math.max(faceRect.width, faceRect.height);
            int sideWithMargin = (int) Math.round(maxSide * 1.2);

            int x = Math.max(0, Math.min(centerX - sideWithMargin / 2, image.width() - 1));
            int y = Math.max(0, Math.min(centerY - sideWithMargin / 2, image.height() - 1));
            int w = Math.min(sideWithMargin, image.width() - x);
            int h = Math.min(sideWithMargin, image.height() - y);
            int side = Math.min(w, h);

            Rect squareRoi = new Rect(x, y, side, side);
            Mat face = new Mat(image, squareRoi);
            try {
                boolean passedQuality = QualityUtils.passesQuality(face, 80, 25.0, 15.0, 240.0);
                if (passedQuality) {
                    Mat resized = new Mat();
                    int interpolation = (face.width() >= 112 || face.height() >= 112)
                            ? Imgproc.INTER_AREA
                            : Imgproc.INTER_CUBIC;
                    Imgproc.resize(face, resized, new Size(112, 112), 0, 0, interpolation);
                    results.add(FaceExtractionOutcome.success(new FaceDetectionResult(resized, faceRect, originalWidth, originalHeight), "Face extracted successfully."));
                } else {
                    logger.debug("[MULTI-FACE DETECTION] Discarded face due to quality check failure");
                }
            } finally {
                face.release();
            }
        }

        logger.info("[MULTI-FACE DETECTION] Successfully processed {} face(s)", results.size());
        logger.info("[MULTI-FACE DETECTION] ========================================");
        return results;
    }
}
