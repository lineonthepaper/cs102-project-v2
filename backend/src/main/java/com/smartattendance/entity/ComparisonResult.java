package com.smartattendance.entity;
public class ComparisonResult {

    private final String faceName;
    private final float similarity;
    private final boolean match;

    public ComparisonResult(String faceName, float similarity, boolean match) {
        this.faceName = faceName;
        this.similarity = similarity;
        this.match = match;
    }

    public String getFaceName() {
        return faceName;
    }

    public float getSimilarity() {
        return similarity;
    }

    public boolean isMatch() {
        return match;
    }

    @Override
    public String toString() {
        return String.format("Face %s: similarity=%.4f, match=%s", faceName, similarity, match);
    }
}