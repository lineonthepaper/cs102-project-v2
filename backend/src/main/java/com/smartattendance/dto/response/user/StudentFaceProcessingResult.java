package com.smartattendance.dto.response.user;

import com.smartattendance.dto.response.face.FaceProcessingSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentFaceProcessingResult {
    private final StudentDTO student;
    private final FaceProcessingSummaryDTO faceProcessingSummary;
}

