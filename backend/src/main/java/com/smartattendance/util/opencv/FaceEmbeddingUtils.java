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
        // ArcFace ResNet100 preprocessing:
        // Input: 112x112 RGB image
        // Normalization: (pixel - 127.5) / 128.0
        // This gives range approximately [-1, 1]
        Mat blob = Dnn.blobFromImage(face, 1.0 / 128.0, new Size(112, 112),
                new Scalar(127.5, 127.5, 127.5), true, false);
        net.setInput(blob);
        Mat output = net.forward();
        return normalizeVector(matToFloatArray(output));
    }

    // Remove unused batch/test CSV utilities; leave only faceToEmbedding API and normalization

    // Utility methods
    private static float[] matToFloatArray(Mat mat) {
        // float[] array = new float[(int) mat.total()];
        // mat.get(0, 0, array);
        try {
            float[] array = FeatureExtraction.predict(mat);
            return array;
        } catch (Exception e) {
            throw new InvalidRequestException(e.getMessage());
        } 
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
