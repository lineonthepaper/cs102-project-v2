package com.smartattendance.entity;

import java.time.OffsetDateTime;

public class FaceProfile {

    private float[] embedding;
    private OffsetDateTime createdAt;

    public FaceProfile() {}

    public FaceProfile(float[] embedding, OffsetDateTime createdAt) {
        this.embedding = embedding;
        this.createdAt = createdAt;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


