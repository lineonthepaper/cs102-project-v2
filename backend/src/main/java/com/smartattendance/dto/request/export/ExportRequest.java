package com.smartattendance.dto.request.export;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class ExportRequest {
    public String userId;
    public String sectionCode;
    public String year;
    public String semester;
    public String role;
}

