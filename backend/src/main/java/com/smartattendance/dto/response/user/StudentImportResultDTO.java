package com.smartattendance.dto.response.user;

import com.smartattendance.dto.response.face.FaceProcessingSummaryDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentImportResultDTO {
    private final boolean success;
    private final String fullName;
    private final String email;
    private final StudentDTO student;
    private final FaceProcessingSummaryDTO faceSummary;
    private final int faceImagesUploaded;
    private final int faceImagesAccepted;
    private final int faceImagesRejected;
    private final String errorMessage;

    public static StudentImportResultDTO success(String fullName,
            String email,
            StudentDTO student,
            FaceProcessingSummaryDTO faceSummary) {
        int totalUploaded = faceSummary != null ? faceSummary.getTotalUploaded() : 0;
        int acceptedCount = faceSummary != null ? faceSummary.getAcceptedCount() : 0;
        int rejectedCount = faceSummary != null ? faceSummary.getRejectedCount()
                : Math.max(0, totalUploaded - acceptedCount);
        return new StudentImportResultDTO(true, fullName, email, student, faceSummary,
                totalUploaded, acceptedCount, rejectedCount, null);
    }

    public static StudentImportResultDTO failure(String fullName,
            String email,
            String errorMessage) {
        return new StudentImportResultDTO(false, fullName, email, null, null,
                0, 0, 0, errorMessage);
    }
}

