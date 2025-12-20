package com.enterprise.reportgenerator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private String id;
    private String jobId;
    private String jobName;
    private String fileName;
    private String filePath;
    private long generatedAt; // Timestamp
    private String status; // SUCCESS, FAILED
    private int rowCount;
    private long fileSizeBytes;
    private String errorMessage; // If failed
}
