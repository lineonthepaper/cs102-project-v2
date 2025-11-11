package com.smartattendance.util.opencv;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import com.smartattendance.exception.InvalidRequestException;

public class FaceEmbeddingUtils {

    private static final Size TARGET_SIZE = new Size(112, 112);
    private static final Scalar ARC_FACE_MEAN = new Scalar(127.5, 127.5, 127.5, 0.0);
    private static final double ARC_FACE_SCALE = 1.0 / 128.0;

    // Convert a single face Mat to embedding
    public static float[] faceToEmbedding(Mat face, Net net) {
        try {
            if (face == null || face.empty()) {
                throw new InvalidRequestException("Face image is empty.");
            }
            if (net == null || net.empty()) {
                throw new InvalidRequestException("Face recognition model is not loaded.");
            }

            Mat resized = new Mat();
            if (face.size().equals(TARGET_SIZE)) {
                face.copyTo(resized);
            } else {
                Imgproc.resize(face, resized, TARGET_SIZE);
            }

            Mat blob = Dnn.blobFromImage(
                    resized,
                    ARC_FACE_SCALE,
                    TARGET_SIZE,
                    ARC_FACE_MEAN,
                    true,
                    false,
                    org.opencv.core.CvType.CV_32F);

            Mat embedding;
            synchronized (net) {
                net.setInput(blob);
                embedding = net.forward();
            }

            float[] raw = new float[(int) embedding.total()];
            embedding.get(0, 0, raw);

            blob.release();
            embedding.release();
            resized.release();

            return normalizeVector(raw);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRequestException("Failed to compute embedding: " + e.getMessage());
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
