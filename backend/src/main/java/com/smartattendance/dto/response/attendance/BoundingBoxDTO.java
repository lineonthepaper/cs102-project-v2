package com.smartattendance.dto.response.attendance;

/**
 * Bounding box coordinates for a detected face.
 */
public class BoundingBoxDTO {
    private int x;
    private int y;
    private int width;
    private int height;
    private int originalWidth;
    private int originalHeight;

    public BoundingBoxDTO() {
    }

    public BoundingBoxDTO(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public BoundingBoxDTO(int x, int y, int width, int height, int originalWidth, int originalHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(int originalWidth) {
        this.originalWidth = originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(int originalHeight) {
        this.originalHeight = originalHeight;
    }
}

