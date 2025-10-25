package com.smartattendance.util.opencv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

public class FaceEmbeddingUtils {
    // Convert a single face Mat to embedding
    public static float[] faceToEmbedding(Mat face, Net net) {
        Mat blob = Dnn.blobFromImage(face, 1.0 / 255.0, new Size(112, 112),
                new Scalar(0.5, 0.5, 0.5), false, false);
        net.setInput(blob);
        Mat output = net.forward();
        return normalizeVector(matToFloatArray(output));
    }

    // Convert multiple face Mats to embeddings
    public static Map<String, float[]> facesToEmbeddings(Map<String, Mat> faces, Net net) {
        Map<String, float[]> embeddings = new HashMap<>();

        Iterator<String> iter = faces.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            Mat face = faces.get(key);
            embeddings.put(key, faceToEmbedding(face, net));
        }

        return embeddings;
    }
        // serialize the embedding to a string that can be stored in the db
    public static String serializeEmbeddingToString(String key, float[] embeddings) {
        StringBuilder sb = new StringBuilder();

        // remove this line once db works
        sb.append(key);

        for (float value : embeddings) {
            sb.append(",").append(value);
        }

        return sb.toString();
    }

    public static Map.Entry<String, float[]> deserializeStringToEmbedding(String serializedEmbedding){
        Map<String, float[]> embeddings = new HashMap<>();

        String[] parts = serializedEmbedding.split(",");
        // remove this once db works
        String key = parts[0];
        float[] embeddingFloats = new float[parts.length - 1];
    
        for (int i = 1; i < parts.length; i++) {
            embeddingFloats[i - 1] = Float.parseFloat(parts[i]);
        }

        embeddings.put(key, embeddingFloats);

        return embeddings.entrySet().iterator().next();
    }

    public static Map<String, float[]> loadEmbeddingsFromCSV(String csvFilePath) {
        Map<String, float[]> embeddings = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty())
                    continue;

                String[] parts = line.split(",");

                // Need at least 2 parts: name + at least 1 embedding value
                if (parts.length >= 2) {
                    String name = parts[0];
                    float[] embedding = new float[parts.length - 1];

                    // Parse each float value
                    for (int i = 1; i < parts.length; i++) {
                        embedding[i - 1] = Float.parseFloat(parts[i]);
                    }

                    embeddings.put(name, embedding);
                }
            }

            // System.out.println("Loaded " + embeddings.size() + " embeddings from: " + csvFilePath);

        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing float values in CSV: " + e.getMessage());
        }

        return embeddings;
    }

    // Utility methods
    private static float[] matToFloatArray(Mat mat) {
        float[] array = new float[(int) mat.total()];
        mat.get(0, 0, array);
        return array;
    }

    private static float[] normalizeVector(float[] vector) {
        float norm = 0.0f;
        for (float v : vector)
            norm += v * v;
        norm = (float) Math.sqrt(norm);

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }

}
