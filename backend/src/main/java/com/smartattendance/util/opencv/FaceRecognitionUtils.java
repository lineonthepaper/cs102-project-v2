package com.smartattendance.util.opencv;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opencv.core.Mat;

import com.smartattendance.entity.ComparisonResult;

public class FaceRecognitionUtils {

    // Default safeguards to reduce false positives (used by compatibility wrapper)
    private static final float DEFAULT_PER_IMAGE_THRESHOLD = 0.5f; // each image must clear this for a "hit"
    private static final int DEFAULT_MIN_HITS_REQUIRED = 1;          // require at least N images to agree per identity
    private static final float DEFAULT_SINGLE_IMAGE_STRICT_THRESHOLD = 0.58f; // stricter if only one reference image
    private static final float DEFAULT_TOP2_MARGIN = 0.05f;          // best must exceed runner-up by this margin


    // Find the best match from training faces (single embedding per identity)
    public static ComparisonResult findBestMatch(Mat targetFace, Map<String, float[]> trainingEmbeddings,
            double similarityThreshold) {
        if (trainingEmbeddings == null || trainingEmbeddings.isEmpty()) {
            return null;
        }

        Map<String, List<float[]>> enrichedEmbeddings = trainingEmbeddings.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

        float[] candidateEmbedding = FaceEmbeddingUtils.faceToEmbedding(targetFace);
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
     * Fine-tunable matching with simplified, clear decision logic.
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

        System.out.println("\n[MATCHING] ==========================================");
        System.out.println("[MATCHING] Thresholds: main=" + String.format("%.2f%%", similarityThreshold * 100) +
                         ", perImage=" + String.format("%.2f%%", perImageThreshold * 100) +
                         ", singleStrict=" + String.format("%.2f%%", singleImageStrictThreshold * 100) +
                         ", margin=" + String.format("%.2f%%", top2Margin * 100));

        // Track top-2 candidates
        float bestSimilarity = Float.NEGATIVE_INFINITY;
        float secondBestSimilarity = Float.NEGATIVE_INFINITY;
        String bestIdentity = null;
        int bestIdentityHits = 0;
        int bestIdentityTotal = 0;

        // Compare against all known identities
        for (Map.Entry<String, List<float[]>> entry : trainingEmbeddings.entrySet()) {
            String identity = entry.getKey();
            List<float[]> embeddings = entry.getValue();
            if (embeddings == null || embeddings.isEmpty()) {
                continue;
            }
            
            float bestSimilarityForIdentity = Float.NEGATIVE_INFINITY;
            int hitsAbove = 0;
            
            System.out.println("[MATCHING] Comparing with " + identity + " (" + embeddings.size() + " reference images):");
            
            for (int i = 0; i < embeddings.size(); i++) {
                float[] embedding = embeddings.get(i);
                if (embedding == null) {
                    continue;
                }
                float similarity = cosine(targetEmbedding, embedding);
                System.out.println(String.format("[MATCHING]   Image %d: %.2f%%", i + 1, similarity * 100));
                
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
            
            // Track top-2 similarities across all identities
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

        // No candidates found at all
        if (bestIdentity == null) {
            System.out.println("[MATCHING] RESULT: No candidates found");
            System.out.println("[MATCHING] ==========================================\n");
            return null;
        }

        // Decision logic - simplified and clear
        System.out.println("\n[MATCHING] Best candidate: " + bestIdentity);
        System.out.println("[MATCHING]   Best similarity: " + String.format("%.2f%%", bestSimilarity * 100));
        System.out.println("[MATCHING]   Runner-up: " + 
                         (secondBestSimilarity == Float.NEGATIVE_INFINITY ? "none" : String.format("%.2f%%", secondBestSimilarity * 100)));
        System.out.println("[MATCHING]   Hits above threshold: " + bestIdentityHits + "/" + bestIdentityTotal);
        
        // Check 1: Main threshold
        boolean passMainThreshold = bestSimilarity >= (float) similarityThreshold;
        System.out.println("[MATCHING]   Check 1 - Main threshold: " + (passMainThreshold ? "PASS" : "FAIL"));
        
        // Check 2: Margin from runner-up
        float margin = secondBestSimilarity == Float.NEGATIVE_INFINITY ? 1.0f : (bestSimilarity - secondBestSimilarity);
        boolean passMargin = margin >= top2Margin;
        System.out.println("[MATCHING]   Check 2 - Margin (" + String.format("%.2f%%", margin * 100) + "): " + 
                         (passMargin ? "PASS" : "FAIL"));
        
        // Check 3: Consensus (multiple images) OR single-image strict threshold
        boolean passConsensus;
        if (bestIdentityTotal <= 1) {
            // Single reference image: require strict threshold
            passConsensus = bestSimilarity >= singleImageStrictThreshold;
            System.out.println("[MATCHING]   Check 3 - Single image strict: " + (passConsensus ? "PASS" : "FAIL"));
        } else {
            // Multiple reference images: require minimum hits
            int required = Math.min(minHitsRequired, bestIdentityTotal);
            passConsensus = bestIdentityHits >= required;
            System.out.println("[MATCHING]   Check 3 - Consensus (" + bestIdentityHits + " >= " + required + "): " + 
                             (passConsensus ? "PASS" : "FAIL"));
        }
        
        // Final decision: ALL checks must pass
        boolean isMatch = passMainThreshold && passMargin && passConsensus;
        
        System.out.println("\n[MATCHING] RESULT: " + (isMatch ? "ACCEPTED ✓" : "REJECTED ✗"));
        System.out.println("[MATCHING] ==========================================\n");
        
        if (!isMatch) {
            return null;
        }

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
                float s = cosine(targetEmbedding, e);
                if (s > bestSimilarity) {
                    bestSimilarity = s;
                    bestIdentity = entry.getKey();
                }
            }
        }
        return bestIdentity == null ? null : new ComparisonResult(bestIdentity, bestSimilarity, false);
    }

    public static float cosine(float[] v1, float[] v2) {
        if (v1 == null || v2 == null) {
            System.out.println("[ERROR] Null vector in similarity!");
            return 0.0f;
        }
        
        if (v1.length != v2.length) {
            System.out.println("[ERROR] Vector length mismatch: " + v1.length + " vs " + v2.length);
            return 0.0f;
        }
        
        float ret = 0.0f;
        float mod1 = 0.0f;
        float mod2 = 0.0f;
        
        for (int i = 0; i < v1.length; i++) {
            ret += v1[i] * v2[i];
            mod1 += v1[i] * v1[i];
            mod2 += v2[i] * v2[i];
        }
        
        // Verify vectors are normalized (magnitude should be ~1.0)
        float denominator = (float) (Math.sqrt(mod1) * Math.sqrt(mod2));
        if (denominator == 0.0f) {
            return 0.0f;
        }
        float cosine = ret / denominator;
        if (cosine > 1.0001f) {
            System.out.println("[WARNING] Cosine similarity > 1 detected (" + cosine + "). Clamping to 1.");
        } else if (cosine < -1.0001f) {
            System.out.println("[WARNING] Cosine similarity < -1 detected (" + cosine + "). Clamping to -1.");
        }
        if (Float.isNaN(cosine) || Float.isInfinite(cosine)) {
            return 0.0f;
        }
        return Math.max(-1.0f, Math.min(1.0f, cosine));
    }

    private static float convertLegacyThreshold(float legacyScore) {
        return Math.max(-1.0f, Math.min(1.0f, legacyScore * 2.0f - 1.0f));
    }

}