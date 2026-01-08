package com.enterprise.reportgenerator.controller;

import com.enterprise.reportgenerator.model.MetricConfig;
import com.enterprise.reportgenerator.repository.JsonMetricRepository;
import com.enterprise.reportgenerator.util.ErrorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class DashboardController {

    private final JsonMetricRepository metricRepository;
    private final com.enterprise.reportgenerator.repository.JsonMetricHistoryRepository historyRepository;
    private final com.enterprise.reportgenerator.service.MetricHistoryService historyService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/metrics")
    public List<MetricConfig> getAllMetrics() {
        return metricRepository.findAll();
    }

    @PostMapping("/metrics")
    public ResponseEntity<MetricConfig> createMetric(@RequestBody MetricConfig metric) {
        if (metric.getId() == null || metric.getId().isEmpty()) {
            metric.setId(UUID.randomUUID().toString());
        }
        metricRepository.save(metric);
        // Collect real data immediately for the new metric
        historyService.collectMetricData(metric.getId());
        return ResponseEntity.ok(metric);
    }

    @DeleteMapping("/metrics/{id}")
    public ResponseEntity<Void> deleteMetric(@PathVariable String id) {
        metricRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<List<com.enterprise.reportgenerator.model.MetricHistory>> getMetricHistory(
            @PathVariable String id) {
        // Return only real collected data
        return ResponseEntity.ok(historyRepository.findByMetricId(id));
    }

    @GetMapping("/data/{id}")
    public ResponseEntity<Object> getMetricData(@PathVariable String id) {
        return metricRepository.findById(id)
                .map(metric -> {
                    try {
                        // Fetch live data from database
                        if (metric.getType() == null || metric.getType().equals("number")) {
                            Object result = jdbcTemplate.queryForObject(metric.getSqlQuery(), Object.class);
                            return ResponseEntity.ok(result);
                        } else {
                            // Assuming list
                            List<Map<String, Object>> result = jdbcTemplate.queryForList(metric.getSqlQuery());
                            return ResponseEntity.ok((Object) result);
                        }
                    } catch (Exception e) {
                        log.error("Error fetching dashboard metric data for '{}': {}", id, e.getMessage());
                        String sanitized = ErrorUtil.sanitizeErrorMessage(e.getMessage());
                        return ResponseEntity.internalServerError().body((Object) sanitized);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
