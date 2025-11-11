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

import com.smartattendance.exception.InvalidRequestException;

import ai.djl.modality.cv.Image;

public class FaceEmbeddingUtils {
    // Convert a single face Mat to embedding
    public static float[] faceToEmbedding(Mat face, Net net) {
        try {
            float[] array = FeatureExtraction.predict(face);
            return normalizeVector(array);
        } catch (Exception e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    // Remove unused batch/test CSV utilities; leave only faceToEmbedding API and normalization

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
