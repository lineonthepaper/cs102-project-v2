package com.smartattendance.util.opencv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    // Find the best match from training faces
    public static ComparisonResult findBestMatch(Mat targetFace, Map<String, float[]> trainingEmbeddings,
            double similarityThreshold, Net net) {
        List<ComparisonResult> results = compareWithTraining(targetFace, trainingEmbeddings, similarityThreshold, net);
        ComparisonResult bestMatch = results.get(0);

        for (ComparisonResult result : results) {
            if (result.getSimilarity() > bestMatch.getSimilarity()) {
                bestMatch = result;
            }
        }

        return bestMatch;
    }

    private static float cosineSimilarity(float[] v1, float[] v2) {
        float dotProduct = 0.0f;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
        }
        return dotProduct;
    }

}