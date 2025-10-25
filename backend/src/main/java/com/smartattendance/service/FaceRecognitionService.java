package com.smartattendance.service;

import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.objdetect.CascadeClassifier;

import org.springframework.stereotype.Service;

import com.smartattendance.entity.ComparisonResult;
import com.smartattendance.util.opencv.FaceRecognitionUtils;
import com.smartattendance.util.helper.ResourcePathUtils;
import com.smartattendance.util.opencv.FaceDetectionUtils;
import com.smartattendance.util.opencv.FaceEmbeddingUtils;

@Service
public class FaceRecognitionService{
    private Net sFaceNet;
    private CascadeClassifier faceDetector;
    private ResourcePathUtils resourceUtils;

    public FaceRecognitionService(ResourcePathUtils resourceUtils){
        this.resourceUtils = resourceUtils;
    }

    public void loadRecognitionModels(){
        sFaceNet = Dnn.readNetFromONNX(resourceUtils.getResourceFilePath("models/face_recognition_sface_2021dec.onnx"));
        faceDetector = new CascadeClassifier(resourceUtils.getResourceFilePath("models/haarcascade_frontalface_alt.xml"));
        if (faceDetector.empty()) {
            System.out.println("Error loading cascade file: " + "./haarcascade_frontalface_alt.xml");
            return;
        }
    }

    public ComparisonResult matchFace(byte[] imageBytes){
        // remove once db is working
        String csvFilePath = resourceUtils.getResourceFilePath("static/trainingEmbeddings.csv");

        Mat face = FaceDetectionUtils.getFaceFromImageBytes(imageBytes, faceDetector);

        if(face == null){
            return null;
        }

        Map<String, float[]> trainingEmbeddings = FaceEmbeddingUtils.loadEmbeddingsFromCSV(csvFilePath);

        ComparisonResult result = FaceRecognitionUtils.findBestMatch(face, trainingEmbeddings, 0.8, sFaceNet);

        return result;
    }
    
}