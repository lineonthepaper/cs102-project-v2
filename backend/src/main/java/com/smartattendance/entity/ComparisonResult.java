package com.smartattendance.entity;

public class ComparisonResult {
    public String faceName;
    public float similarity;
    public boolean isMatch;

    public ComparisonResult(String faceName, float similarity, boolean isMatch) {
        this.faceName = faceName;
        this.similarity = similarity;
        this.isMatch = isMatch;
    }

    public float getSimilarity() {
        return similarity;
    }

    @Override
    public String toString() {
        return String.format("Face %s: similarity=%.4f, match=%s",
                faceName, similarity, isMatch);
    }
}