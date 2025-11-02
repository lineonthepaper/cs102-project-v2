package com.smartattendance.dto.response.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bounding box coordinates for a detected face.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBoxDTO {
    private int x;
    private int y;
    private int width;
    private int height;
    private int originalWidth;
    private int originalHeight;

    public BoundingBoxDTO(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}

