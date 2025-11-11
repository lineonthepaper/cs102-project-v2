package com.smartattendance.dto.response.face;

import com.smartattendance.util.opencv.FaceExtractionError;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FaceImageProcessingResultDTO {

    private final int index;
    private final boolean accepted;
    private final String message;
    private final FaceExtractionError errorCode;

    public static FaceImageProcessingResultDTO accepted(int index, String message) {
        return new FaceImageProcessingResultDTO(index, true, message, null);
    }

    public static FaceImageProcessingResultDTO rejected(int index,
                                                        FaceExtractionError errorCode,
                                                        String message) {
        return new FaceImageProcessingResultDTO(index, false, message, errorCode);
    }
}

