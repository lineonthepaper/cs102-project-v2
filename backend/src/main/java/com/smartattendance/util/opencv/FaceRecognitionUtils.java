package com.smartattendance.util.opencv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opencv.core.Mat;
import org.opencv.dnn.Net;

import com.smartattendance.entity.ComparisonResult;

public class FaceRecognitionUtils {

    // Compare target embedding with all training embeddings
    private static List<ComparisonResult> compareWithTraining(Mat targetFace, Map<String, float[]> trainingEmbeddings,
            double similarityThreshold, Net net) {
        float[] targetEmbedding = FaceEmbeddingUtils.faceToEmbedding(targetFace, net);
        
        List<ComparisonResult> results = new ArrayList<>();
        Iterator<String> iter = trainingEmbeddings.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            float similarity = cosineSimilarity(targetEmbedding, trainingEmbeddings.get(key));
            // System.out.println(key + ":" + similarity + "%");
            results.add(new ComparisonResult(key, similarity, similarity >= similarityThreshold));

        }

        return results;
    }

    // Find the best match from training faces (single embedding per identity)
    public static ComparisonResult findBestMatch(Mat targetFace, Map<String, float[]> trainingEmbeddings,
            double similarityThreshold, Net net) {
        if (trainingEmbeddings == null || trainingEmbeddings.isEmpty()) {
            return null;
        }

        Map<String, List<float[]>> enrichedEmbeddings = trainingEmbeddings.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

        float[] candidateEmbedding = FaceEmbeddingUtils.faceToEmbedding(targetFace, net);
        return findBestMatch(candidateEmbedding, enrichedEmbeddings, similarityThreshold);
    }

    /**
     * Find the best match when each identity can have multiple embeddings.
     */
    public static ComparisonResult findBestMatch(float[] targetEmbedding,
            Map<String, List<float[]>> trainingEmbeddings,
            double similarityThreshold) {

        if (trainingEmbeddings == null || trainingEmbeddings.isEmpty()) {
            return null;
        }

        ComparisonResult bestMatch = null;
        System.out.println("[DEBUG] === Face Recognition Comparison ===");
        System.out.println("[DEBUG] Threshold: " + String.format("%.2f%%", similarityThreshold * 100));

        for (Map.Entry<String, List<float[]>> entry : trainingEmbeddings.entrySet()) {
            String identity = entry.getKey();
            List<float[]> embeddings = entry.getValue();
            if (embeddings == null || embeddings.isEmpty()) {
                continue;
            }

            float bestSimilarityForIdentity = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < embeddings.size(); i++) {
                float[] embedding = embeddings.get(i);
                if (embedding == null) {
                    continue;
                }
                float similarity = cosineSimilarity(targetEmbedding, embedding);
                System.out.println(String.format("[DEBUG]   %s (image %d): %.2f%%", 
                                                identity, i + 1, similarity * 100));
                if (similarity > bestSimilarityForIdentity) {
                    bestSimilarityForIdentity = similarity;
                }
            }

            if (bestSimilarityForIdentity == Float.NEGATIVE_INFINITY) {
                continue;
            }

            ComparisonResult candidate = new ComparisonResult(identity, bestSimilarityForIdentity,
                    bestSimilarityForIdentity >= similarityThreshold);

            if (bestMatch == null || candidate.getSimilarity() > bestMatch.getSimilarity()) {
                bestMatch = candidate;
            }
        }

        if (bestMatch != null) {
            System.out.println(String.format("[DEBUG] BEST MATCH: %s at %.2f%% (threshold: %.2f%%)", 
                                            bestMatch.getFaceName(), 
                                            bestMatch.getSimilarity() * 100,
                                            similarityThreshold * 100));
        } else {
            System.out.println("[DEBUG] No match above threshold");
        }
        System.out.println("[DEBUG] ===================================");

        return bestMatch;
    }

    private static float cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null) {
            System.out.println("[ERROR] Null vector in cosineSimilarity!");
            return 0.0f;
        }
        
        if (v1.length != v2.length) {
            System.out.println("[ERROR] Vector length mismatch: " + v1.length + " vs " + v2.length);
            return 0.0f;
        }
        
        float dotProduct = 0.0f;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
        }
        
        // Verify vectors are normalized (magnitude should be ~1.0)
        if (dotProduct > 1.01f || dotProduct < -1.01f) {
            System.out.println("[WARNING] Dot product out of range for normalized vectors: " + dotProduct);
            System.out.println("[WARNING] This suggests vectors are not properly normalized!");
        }
        
        return dotProduct;
    }

}