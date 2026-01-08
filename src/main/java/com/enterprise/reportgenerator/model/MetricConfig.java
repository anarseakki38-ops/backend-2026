package com.enterprise.reportgenerator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricConfig {
    private String id;
    private String title;
    private String sqlQuery;
    private String type; // e.g., "number", "list" (for now just number)
    private String icon; // Icon name from Lucide
}
