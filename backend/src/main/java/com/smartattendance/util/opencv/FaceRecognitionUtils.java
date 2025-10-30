package com.smartattendance.util.opencv;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opencv.core.Mat;
import org.opencv.dnn.Net;

import com.smartattendance.entity.ComparisonResult;

public class FaceRecognitionUtils {

    // Default safeguards to reduce false positives (used by compatibility wrapper)
    private static final float DEFAULT_PER_IMAGE_THRESHOLD = 0.82f; // each image must clear this for a "hit"
    private static final int DEFAULT_MIN_HITS_REQUIRED = 1;          // require at least N images to agree per identity
    private static final float DEFAULT_SINGLE_IMAGE_STRICT_THRESHOLD = 0.90f; // stricter if only one reference image
    private static final float DEFAULT_TOP2_MARGIN = 0.02f;          // best must exceed runner-up by this margin


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

        // Backward-compatible wrapper with default strict settings
        return findBestMatch(
            targetEmbedding,
            trainingEmbeddings,
            similarityThreshold,
            DEFAULT_PER_IMAGE_THRESHOLD,
            DEFAULT_MIN_HITS_REQUIRED,
            DEFAULT_SINGLE_IMAGE_STRICT_THRESHOLD,
            DEFAULT_TOP2_MARGIN
        );
    }

    /**
     * Fine-tunable matching that applies threshold, consensus and top-2 margin checks.
     */
    public static ComparisonResult findBestMatch(float[] targetEmbedding,
            Map<String, List<float[]>> trainingEmbeddings,
            double similarityThreshold,
            float perImageThreshold,
            int minHitsRequired,
            float singleImageStrictThreshold,
            float top2Margin) {

        if (trainingEmbeddings == null || trainingEmbeddings.isEmpty()) {
            return null;
        }

        // track top-2 and consensus only; do not allocate interim object until acceptance
        float bestSimilarity = Float.NEGATIVE_INFINITY;
        float secondBestSimilarity = Float.NEGATIVE_INFINITY;
        String bestIdentity = null;
        int bestIdentityHits = 0;
        int bestIdentityTotal = 0;
        System.out.println("[DEBUG] === Face Recognition Comparison ===");
        System.out.println("[DEBUG] Threshold: " + String.format("%.2f%%", similarityThreshold * 100));

        for (Map.Entry<String, List<float[]>> entry : trainingEmbeddings.entrySet()) {
            String identity = entry.getKey();
            List<float[]> embeddings = entry.getValue();
            if (embeddings == null || embeddings.isEmpty()) {
                continue;
            }
            float bestSimilarityForIdentity = Float.NEGATIVE_INFINITY;
            int hitsAbove = 0;
            for (int i = 0; i < embeddings.size(); i++) {
                float[] embedding = embeddings.get(i);
                if (embedding == null) {
                    continue;
                }
                float similarity = cosineSimilarity(targetEmbedding, embedding);
                System.out.println(String.format("[DEBUG]   %s (image %d): %.2f%%", 
                                                identity, i + 1, similarity * 100));
                if (similarity >= perImageThreshold) {
                    hitsAbove++;
                }
                if (similarity > bestSimilarityForIdentity) {
                    bestSimilarityForIdentity = similarity;
                }
            }

            if (bestSimilarityForIdentity == Float.NEGATIVE_INFINITY) {
                continue;
            }
            // Track top-2 similarities across identities for margin check
            if (bestSimilarityForIdentity > bestSimilarity) {
                secondBestSimilarity = bestSimilarity;
                bestSimilarity = bestSimilarityForIdentity;
                bestIdentity = identity;
                bestIdentityHits = hitsAbove;
                bestIdentityTotal = embeddings.size();
            } else if (bestSimilarityForIdentity > secondBestSimilarity) {
                secondBestSimilarity = bestSimilarityForIdentity;
            }
        }

        // Final decision with stricter rules
        if (bestIdentity == null) {
            System.out.println("[DEBUG] No match candidates found");
            System.out.println("[DEBUG] ===================================");
            return null;
        }

        boolean clearsMainThreshold = bestSimilarity >= (float) similarityThreshold;
        boolean clearsMargin = secondBestSimilarity == Float.NEGATIVE_INFINITY || (bestSimilarity - secondBestSimilarity) >= top2Margin;

        boolean clearsConsensus;
        if (bestIdentityTotal <= 1) {
            // If only one reference image, require very strict single-image threshold
            clearsConsensus = bestSimilarity >= singleImageStrictThreshold;
        } else {
            // Cap the requirement by how many references we actually have
            int required = Math.min(minHitsRequired, bestIdentityTotal);
            clearsConsensus = bestIdentityHits >= required;
        }

        System.out.println(String.format("[DEBUG] BEST CANDIDATE: %s at %.2f%% | runner-up: %.2f%% | hits: %d/%d", 
                                         bestIdentity, bestSimilarity * 100, 
                                         (secondBestSimilarity==Float.NEGATIVE_INFINITY?0:secondBestSimilarity*100),
                                         bestIdentityHits, bestIdentityTotal));
        System.out.println(String.format("[DEBUG] Checks -> main: %b, margin: %b, consensus: %b", 
                                         clearsMainThreshold, clearsMargin, clearsConsensus));

        // Adaptive acceptance: allow high-confidence or strong-margin matches
        boolean isHighConfidence = bestSimilarity >= Math.max(singleImageStrictThreshold, (float) similarityThreshold + 0.05f);
        boolean isStrongMargin = clearsMargin && bestSimilarity >= (float) similarityThreshold - 0.02f && (bestSimilarity - secondBestSimilarity) >= Math.max(top2Margin, 0.05f);
        boolean isMatch = (clearsMainThreshold && clearsMargin && clearsConsensus) || isHighConfidence || isStrongMargin;
        if (!isMatch) {
            System.out.println("[DEBUG] Final decision: REJECTED");
            System.out.println("[DEBUG] ===================================");
            return null;
        }

        System.out.println("[DEBUG] Final decision: ACCEPTED");
        System.out.println("[DEBUG] ===================================");
        return new ComparisonResult(bestIdentity, bestSimilarity, true);
    }

    /**
     * Return the best candidate identity by raw cosine similarity (no gates).
     */
    public static ComparisonResult findTopCandidate(float[] targetEmbedding,
            Map<String, List<float[]>> trainingEmbeddings) {
        if (trainingEmbeddings == null || trainingEmbeddings.isEmpty()) {
            return null;
        }
        String bestIdentity = null;
        float bestSimilarity = Float.NEGATIVE_INFINITY;
        for (Map.Entry<String, List<float[]>> entry : trainingEmbeddings.entrySet()) {
            List<float[]> embeddings = entry.getValue();
            if (embeddings == null || embeddings.isEmpty()) continue;
            for (float[] e : embeddings) {
                if (e == null) continue;
                float s = cosineSimilarity(targetEmbedding, e);
                if (s > bestSimilarity) {
                    bestSimilarity = s;
                    bestIdentity = entry.getKey();
                }
            }
        }
        return bestIdentity == null ? null : new ComparisonResult(bestIdentity, bestSimilarity, false);
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