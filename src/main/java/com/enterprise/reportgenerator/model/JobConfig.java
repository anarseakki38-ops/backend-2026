package com.enterprise.reportgenerator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobConfig {
    private String id;
    private String name;
    private String cronExpression;
    private String sqlFileName;
    private String sqlContent; // Payload for creating/updating SQL file
    private boolean emailEnabled;
    private List<String> emailRecipients;
    private boolean enabled = true; // Job is active by default
    private String targetDatabase = "PRIMARY"; // PRIMARY or SECONDARY
    private String fromDate; // Optional: For queries with :FromDate parameter (format: yyyy-MM-dd)
    private String toDate; // Optional: For queries with :ToDate parameter (format: yyyy-MM-dd)
    private DashboardMapping dashboardMapping;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardMapping {
        private boolean enabled;
        private List<MetricConfig> metrics;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricConfig {
        private String type; // COUNTER, LINE_CHART, BAR_CHART
        private String column; // Column name in SQL result
        private String label;
        private String xAxis; // For charts
        private String yAxis; // For charts
    }
}
