package com.smartattendance.util.opencv;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.translator.ImageFeatureExtractorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.training.util.DownloadUtils;
import ai.djl.ndarray.types.Shape;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.image.BufferedImage;

import org.opencv.core.Mat;

import com.smartattendance.util.converter.MatConverter;

public class FeatureExtraction {
    public static float[] predict(Mat mat)
            throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromImage(MatConverter.matToBufferedImage(mat));
        img.getWrappedImage();

        List<Float> mean =
                Arrays.asList(
                        127.5f / 255.0f,
                        127.5f / 255.0f,
                        127.5f / 255.0f,
                        128.0f / 255.0f,
                        128.0f / 255.0f,
                        128.0f / 255.0f);

        String normalize = mean.stream().map(Object::toString).collect(Collectors.joining(","));

        Criteria<Image, float[]> criteria =
                Criteria.builder()
                        .setTypes(Image.class, float[].class)
                        .optModelPath(Paths.get("src/main/resources/pytorch_models/face_feature.zip"))
                        .optModelName("face_feature") 
                        .optArgument("normalize", normalize)
                        .optTranslatorFactory(new ImageFeatureExtractorFactory())
                        .optProgress(new ProgressBar())
                        .optEngine("PyTorch") // Use PyTorch engine
                        .build();

        try (ZooModel<Image, float[]> model = criteria.loadModel()) {
            Predictor<Image, float[]> predictor = model.newPredictor();
            return predictor.predict(img);
        }
    }
}
