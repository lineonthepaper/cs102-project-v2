package com.smartattendance.service;

/**
 * Simple value object describing a face recognition match.
 */
public class FaceMatch {

    private final String studentId;
    private final double similarity;

    public FaceMatch(String studentId, double similarity) {
        this.studentId = studentId;
        this.similarity = similarity;
    }

    public String getStudentId() {
        return studentId;
    }

    public double getSimilarity() {
        return similarity;
    }
}

