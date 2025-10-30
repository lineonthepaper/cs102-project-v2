package com.smartattendance.util.opencv;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

public class QualityUtils {

    private QualityUtils() {}

    public static double varianceOfLaplacian(Mat imageBgr) {
        Mat gray = new Mat();
        Imgproc.cvtColor(imageBgr, gray, Imgproc.COLOR_BGR2GRAY);
        Mat lap = new Mat();
        Imgproc.Laplacian(gray, lap, 3);
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        org.opencv.core.Core.meanStdDev(lap, mean, std);
        double variance = Math.pow(std.get(0,0)[0], 2.0);
        gray.release();
        lap.release();
        return variance;
    }

    public static double meanBrightness(Mat imageBgr) {
        Mat gray = new Mat();
        Imgproc.cvtColor(imageBgr, gray, Imgproc.COLOR_BGR2GRAY);
        double mean = org.opencv.core.Core.mean(gray).val[0];
        gray.release();
        return mean; // 0-255
    }

    public static boolean passesQuality(Mat roiBgr, int minSidePx, double minLaplacianVar, double minBrightness, double maxBrightness) {
        if (roiBgr == null || roiBgr.empty()) {
            System.out.println("[QUALITY] Failed: null or empty Mat");
            return false;
        }
        
        if (roiBgr.width() < minSidePx || roiBgr.height() < minSidePx) {
            System.out.println("[QUALITY] Failed: size " + roiBgr.width() + "x" + roiBgr.height() + " < " + minSidePx + "px");
            return false;
        }
        
        double var = varianceOfLaplacian(roiBgr);
        if (var < minLaplacianVar) {
            System.out.println("[QUALITY] Failed: blur variance " + String.format("%.2f", var) + " < " + minLaplacianVar);
            return false;
        }
        
        double mean = meanBrightness(roiBgr);
        if (mean < minBrightness || mean > maxBrightness) {
            System.out.println("[QUALITY] Failed: brightness " + String.format("%.1f", mean) + " outside range [" + minBrightness + ", " + maxBrightness + "]");
            return false;
        }
        
        System.out.println("[QUALITY] Passed: size=" + roiBgr.width() + "x" + roiBgr.height() + 
                         ", blur=" + String.format("%.1f", var) + ", brightness=" + String.format("%.1f", mean));
        return true;
    }
}


