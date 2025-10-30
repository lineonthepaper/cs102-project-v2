package com.smartattendance.util.opencv;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;

public class FaceDetectionUtils {

    public static Mat getFaceFromImageBytes(byte[] imageBytes, CascadeClassifier faceDetector) {
        // Convert bytes to Mat
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        System.out.println("[DEBUG] Image decoded: " + image.width() + "x" + image.height());

        // Convert to grayscale for better face detection
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        // Detect faces with better parameters
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(
            gray, 
            faceDetections,
            1.1,  // scaleFactor
            3,    // minNeighbors
            0,    // flags
            new Size(30, 30),  // minSize
            new Size()  // maxSize (empty = no limit)
        );

        // Extract and prepare faces for SFace
        Rect[] faces = faceDetections.toArray();
        System.out.println("[DEBUG] Faces detected: " + faces.length);
        
        if (faces.length == 0) {
            System.out.println("[DEBUG] No face detected in image");
            return null;
        }

        if (faces.length > 1) {
            System.out.println("[DEBUG] WARNING: Multiple faces detected (" + faces.length + "), using largest one");
            // Use the largest face
            Rect largest = faces[0];
            for (Rect face : faces) {
                if (face.width * face.height > largest.width * largest.height) {
                    largest = face;
                }
            }
            faces[0] = largest;
        }

        Rect faceRect = faces[0];
        System.out.println(String.format("[DEBUG] Face location: x=%d, y=%d, w=%d, h=%d", 
                                        faceRect.x, faceRect.y, faceRect.width, faceRect.height));

        // Extract face from original color image (not grayscale)
        Mat face = new Mat(image, faceRect);
        
        // Resize with high quality interpolation
        Mat resizedFace = new Mat();
        Imgproc.resize(face, resizedFace, new Size(112, 112), 0, 0, Imgproc.INTER_CUBIC);
        System.out.println("[DEBUG] Face resized to 112x112 for embedding");
        
        // DEBUG: Save the detected face for verification
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String debugPath = "/tmp/detected_face_" + timestamp + ".jpg";
            Imgcodecs.imwrite(debugPath, resizedFace);
            System.out.println("[DEBUG] Saved detected face to: " + debugPath);
        } catch (Exception e) {
            System.out.println("[DEBUG] Could not save debug image: " + e.getMessage());
        }

        return resizedFace;
    }

    // for testing, will remove once db works
    public static Map<String, Mat> getFacesFromDirectory(String directoryPath, CascadeClassifier faceDetector) {
        Map<String, Mat> allFaces = new HashMap<>();

        File dir = new File(directoryPath);
        File[] imageFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg") ||
                name.toLowerCase().endsWith(".png"));

        if (imageFiles == null) {
            System.out.println("No images found in directory: " + directoryPath);
            return allFaces;
        }

        for (File imageFile : imageFiles) {
            try {
                byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                Mat face = getFaceFromImageBytes(imageBytes, faceDetector);

                if (face != null) {
                    // Remove file extension from the filename
                    String fileName = imageFile.getName();
                    String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
                    allFaces.put(fileNameWithoutExt, face);
                    System.out.println("Processed " + imageFile.getName());
                } else {
                    System.out.println("No face found in " + imageFile.getName());
                }

            } catch (Exception e) {
                System.err.println("Error processing " + imageFile.getName() + ": " + e.getMessage());
            }
        }

        return allFaces;
    }
}
