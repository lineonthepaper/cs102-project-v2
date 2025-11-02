package com.smartattendance.util.opencv;

import org.opencv.core.*;
import org.opencv.dnn.Net;
import org.opencv.dnn.Dnn;
import java.util.ArrayList;
import java.util.List;

public class OnnxFaceDetector {
    private final Net net;
    private final Size detSize = new Size(640, 640); // Default SCRFD size
    public static class FaceWithLandmarks {
        public Rect bbox;
        public Point[] landmarks; // [right eye, left eye, nose, right mouth, left mouth]
        public FaceWithLandmarks(Rect bbox, Point[] landmarks) {
            this.bbox = bbox;
            this.landmarks = landmarks;
        }
    }
    public OnnxFaceDetector(String modelPath) {
        Net model = null;
        try { model = Dnn.readNetFromONNX(modelPath); } catch (Exception ignore) {}
        this.net = model;
    }
    public boolean isLoaded() { return net != null; }
    
    // Detect all faces and return count with best face
    public static class MultiFaceResult {
        public final FaceWithLandmarks bestFace;
        public final int faceCount;
        public MultiFaceResult(FaceWithLandmarks bestFace, int faceCount) {
            this.bestFace = bestFace;
            this.faceCount = faceCount;
        }
    }
    
    // Detect first/highest-confidence face and use its 5 landmarks
    public FaceWithLandmarks detect(Mat image) {
        MultiFaceResult result = detectMulti(image);
        return result != null ? result.bestFace : null;
    }
    
    // Detect all faces, return best face + count
    public MultiFaceResult detectMulti(Mat image) {
        if (net == null) return null;
        // Preprocess: resize, pad, normalize
        Mat blob = Dnn.blobFromImage(image, 1.0/128.0, detSize, new Scalar(127.5,127.5,127.5), true, false);
        net.setInput(blob);
        List<Mat> outputs = new ArrayList<>();
        List<String> outNames = new ArrayList<>();
        outNames.add("output0"); outNames.add("output1");
        net.forward(outputs, outNames);
        if (outputs.size() < 2) return null;
        Mat bboxes = outputs.get(0); // [N,4] xywh
        Mat landmarks = outputs.get(1); // [N,10] (2x5: each xy pair for one pt)
        int num = bboxes.rows();
        if (num < 1) return null;
        // Find largest or best by width*height
        int bestIdx = -1; double maxArea=0;
        for (int i=0;i<num;i++) {
            double w = bboxes.get(i,2)[0], h = bboxes.get(i,3)[0];
            double area = w * h;
            if (area > maxArea) { maxArea=area; bestIdx=i; }
        }
        if (bestIdx<0) return null;
        // recover original scale
        double x = bboxes.get(bestIdx,0)[0], y=bboxes.get(bestIdx,1)[0], w=bboxes.get(bestIdx,2)[0], h=bboxes.get(bestIdx,3)[0];
        Rect bbox = new Rect((int)x,(int)y, (int)w, (int)h);
        Point[] points = new Point[5];
        for (int p=0;p<5;p++) points[p]=new Point(
            landmarks.get(bestIdx,p*2)[0], landmarks.get(bestIdx,p*2+1)[0]);
        return new MultiFaceResult(new FaceWithLandmarks(bbox, points), num);
    }
    
    // Detect all faces, return list of all faces with their bounding boxes
    public List<FaceWithLandmarks> detectAllFaces(Mat image) {
        List<FaceWithLandmarks> faces = new ArrayList<>();
        if (net == null) return faces;
        
        // Preprocess: resize, pad, normalize
        Mat blob = Dnn.blobFromImage(image, 1.0/128.0, detSize, new Scalar(127.5,127.5,127.5), true, false);
        net.setInput(blob);
        List<Mat> outputs = new ArrayList<>();
        List<String> outNames = new ArrayList<>();
        outNames.add("output0"); outNames.add("output1");
        net.forward(outputs, outNames);
        if (outputs.size() < 2) return faces;
        
        Mat bboxes = outputs.get(0); // [N,4] xywh
        Mat landmarks = outputs.get(1); // [N,10] (2x5: each xy pair for one pt)
        int num = bboxes.rows();
        
        System.out.println("[ONNX DETECT] Raw detector found " + num + " faces before processing");
        
        // Extract all faces
        for (int i = 0; i < num; i++) {
            double x = bboxes.get(i,0)[0], y = bboxes.get(i,1)[0], w = bboxes.get(i,2)[0], h = bboxes.get(i,3)[0];
            Rect bbox = new Rect((int)x, (int)y, (int)w, (int)h);
            Point[] points = new Point[5];
            for (int p = 0; p < 5; p++) {
                points[p] = new Point(
                    landmarks.get(i, p*2)[0], 
                    landmarks.get(i, p*2+1)[0]
                );
            }
            faces.add(new FaceWithLandmarks(bbox, points));
        }
        
        System.out.println("[ONNX DETECT] Returning " + faces.size() + " faces");
        return faces;
    }
}
