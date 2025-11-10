package com.smartattendance.util.opencv;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import com.smartattendance.util.opencv.MatConversionUtils;

import org.opencv.core.Mat;

public final class LightFaceDetection {

    private static final Logger logger = LoggerFactory.getLogger(LightFaceDetection.class);

    private LightFaceDetection() {}

    public static DetectedObjects predict(Mat mat) throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromImage(MatConversionUtils.matToBufferedImage(mat));

        double confThresh = 0.95f;
        double nmsThresh = 0.45f;
        double[] variance = {0.1f, 0.2f};
        int topK = 5000;
        int[][] scales = {{10, 16, 24}, {32, 48}, {64, 96}, {128, 192, 256}};
        int[] steps = {8, 16, 32, 64};

        FaceDetectionTranslator translator =
                new FaceDetectionTranslator(confThresh, nmsThresh, variance, topK, scales, steps);

        Criteria<Image, DetectedObjects> criteria =
                Criteria.builder()
                        .setTypes(Image.class, DetectedObjects.class)
                        .optModelUrls("src/main/resources/pytorch_models/ultranet.zip")
                        .optModelName("ultranet") 
                        .optTranslator(translator)
                        .optProgress(new ProgressBar())
                        .optEngine("PyTorch") // Use PyTorch engine
                        .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel()) {
            try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
                DetectedObjects detection = predictor.predict(img);
                logger.info("detection: {}", detection);
                return detection;
            }
        }
    }
}