package com.smartattendance.dto.response.attendance;

import lombok.Data;

/**
 * Lightweight DTO describing a student detected by the face recognition flow.
 */
@Data
public class RecognizedStudentDTO {

    private String id;
    private String displayId;
    private String firstName;
    private String lastName;
    private String email;

}

