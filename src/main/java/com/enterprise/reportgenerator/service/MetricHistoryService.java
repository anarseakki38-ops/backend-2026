package com.enterprise.reportgenerator.service;

import com.enterprise.reportgenerator.model.MetricConfig;
import com.enterprise.reportgenerator.model.MetricHistory;
import com.enterprise.reportgenerator.repository.JsonMetricHistoryRepository;
import com.enterprise.reportgenerator.repository.JsonMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricHistoryService {

    private final JsonMetricRepository metricRepository;
    private final JsonMetricHistoryRepository historyRepository;
    private final JdbcTemplate jdbcTemplate;

    // Collect real data every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void collectMetrics() {
        log.info("Collecting metric history from database...");
        List<MetricConfig> metrics = metricRepository.findAll();
        long now = System.currentTimeMillis();

        for (MetricConfig metric : metrics) {
            try {
                if (metric.getType() == null || metric.getType().equals("number")) {
                    Number val = jdbcTemplate.queryForObject(metric.getSqlQuery(), Number.class);
                    if (val != null) {
                        historyRepository.add(new MetricHistory(metric.getId(), now, val.doubleValue()));
                        log.info("Collected data for metric '{}': {}", metric.getTitle(), val.doubleValue());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to collect history for metric '{}': {}", metric.getTitle(), e.getMessage());
            }
        }
    }

    // Manually trigger collection (called when new metric is created)
    public void collectMetricData(String metricId) {
        MetricConfig metric = metricRepository.findById(metricId).orElse(null);
        if (metric == null) {
            log.warn("Metric not found for id: {}", metricId);
            return;
        }

        try {
            if (metric.getType() == null || metric.getType().equals("number")) {
                Number val = jdbcTemplate.queryForObject(metric.getSqlQuery(), Number.class);
                if (val != null) {
                    long now = System.currentTimeMillis();
                    historyRepository.add(new MetricHistory(metric.getId(), now, val.doubleValue()));
                    log.info("Manually collected data for metric '{}': {}", metric.getTitle(), val.doubleValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to manually collect data for metric '{}': {}", metric.getTitle(), e.getMessage());
        }
    }
}
