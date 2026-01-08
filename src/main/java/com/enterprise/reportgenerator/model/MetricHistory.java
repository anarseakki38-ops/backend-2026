package com.enterprise.reportgenerator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricHistory {
    private String metricId;
    private long timestamp;
    private double value;
}
