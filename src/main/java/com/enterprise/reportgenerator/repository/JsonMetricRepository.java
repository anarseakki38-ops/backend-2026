package com.enterprise.reportgenerator.repository;

import com.enterprise.reportgenerator.model.MetricConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class JsonMetricRepository {

    @Value("${app.config.path:data/config}")
    private String configPath;

    private final String CONFIG_FILE = "metrics.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<MetricConfig> metrics = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        loadMetrics();
    }

    private void loadMetrics() {
        try {
            File file = new File(configPath, CONFIG_FILE);
            if (file.exists() && file.length() > 0) {
                List<MetricConfig> loaded = objectMapper.readValue(file, new TypeReference<List<MetricConfig>>() {
                });
                metrics.clear();
                if (loaded != null) {
                    metrics.addAll(loaded);
                }
            } else {
                metrics.clear(); // Ensure empty if file is empty
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Start with empty list if file is corrupt
            metrics.clear();
        }
    }

    private void saveMetrics() {
        try {
            File dir = new File(configPath);
            if (!dir.exists())
                dir.mkdirs();
            File file = new File(dir, CONFIG_FILE);
            objectMapper.writeValue(file, metrics);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<MetricConfig> findAll() {
        return new ArrayList<>(metrics);
    }

    public Optional<MetricConfig> findById(String id) {
        return metrics.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    public MetricConfig save(MetricConfig metric) {
        metrics.removeIf(m -> m.getId().equals(metric.getId()));
        metrics.add(metric);
        saveMetrics();
        return metric;
    }

    public void deleteById(String id) {
        metrics.removeIf(m -> m.getId().equals(id));
        saveMetrics();
    }
}
