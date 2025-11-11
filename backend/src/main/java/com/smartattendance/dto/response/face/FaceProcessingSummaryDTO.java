package com.smartattendance.dto.response.face;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public class FaceProcessingSummaryDTO {

    private final int totalUploaded;
    private final int acceptedCount;
    private final int rejectedCount;
    private final List<FaceImageProcessingResultDTO> results;

    public FaceProcessingSummaryDTO(int totalUploaded,
                                    int acceptedCount,
                                    int rejectedCount,
                                    List<FaceImageProcessingResultDTO> results) {
        this.totalUploaded = totalUploaded;
        this.acceptedCount = acceptedCount;
        this.rejectedCount = rejectedCount;
        this.results = results == null ? Collections.emptyList() : List.copyOf(results);
    }

    public boolean hasFailures() {
        return rejectedCount > 0;
    }

    public static FaceProcessingSummaryDTO from(int totalUploaded,
                                                int acceptedCount,
                                                List<FaceImageProcessingResultDTO> results) {
        int rejected = Math.max(0, totalUploaded - acceptedCount);
        return new FaceProcessingSummaryDTO(totalUploaded, acceptedCount, rejected, results);
    }
}

