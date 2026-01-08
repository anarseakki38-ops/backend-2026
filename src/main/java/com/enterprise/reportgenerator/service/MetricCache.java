package com.enterprise.reportgenerator.service;

import com.enterprise.reportgenerator.model.JobConfig;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MetricCache {

    // Key: JobID, Value: List of calculated metrics
    private final Map<String, List<MetricValue>> cache = new ConcurrentHashMap<>();

    public void updateMetrics(String jobId, List<MetricValue> metrics) {
        cache.put(jobId, metrics);
    }

    public Map<String, List<MetricValue>> getAllMetrics() {
        return Collections.unmodifiableMap(cache);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MetricValue {
        private String label;
        private Object value;
        private String type; // COUNTER, CHART
        private JobConfig.MetricConfig originalConfig;
    }
}
