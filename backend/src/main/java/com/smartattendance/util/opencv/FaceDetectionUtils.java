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

        // Detect faces
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(image, faceDetections);

        // Extract and prepare faces for SFace
        Rect[] faces = faceDetections.toArray();
        if (faces.length == 0) {
            return null;
        }

        Mat face = new Mat(image, faces[0]);
        Mat resizedFace = new Mat();
        Imgproc.resize(face, resizedFace, new Size(112, 112));

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
