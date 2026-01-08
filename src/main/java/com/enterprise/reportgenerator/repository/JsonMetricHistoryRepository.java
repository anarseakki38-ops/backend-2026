package com.enterprise.reportgenerator.repository;

import com.enterprise.reportgenerator.model.MetricHistory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Repository
public class JsonMetricHistoryRepository {

    @Value("${app.config.path:data/config}")
    private String configPath;

    private final String HISTORY_FILE = "history.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<MetricHistory> history = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        loadHistory();
    }

    private void loadHistory() {
        try {
            File file = new File(configPath, HISTORY_FILE);
            if (file.exists() && file.length() > 0) {
                List<MetricHistory> loaded = objectMapper.readValue(file, new TypeReference<List<MetricHistory>>() {
                });
                if (loaded != null) {
                    history.addAll(loaded);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveHistory() {
        try {
            File dir = new File(configPath);
            if (!dir.exists())
                dir.mkdirs();
            File file = new File(dir, HISTORY_FILE);
            objectMapper.writeValue(file, history);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void add(MetricHistory entry) {
        history.add(entry);
        saveHistory();
    }

    public void addAll(List<MetricHistory> entries) {
        history.addAll(entries);
        saveHistory();
    }

    public List<MetricHistory> findByMetricId(String metricId) {
        return history.stream()
                .filter(h -> h.getMetricId().equals(metricId))
                .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                .collect(Collectors.toList());
    }

    public boolean hasHistory(String metricId) {
        return history.stream().anyMatch(h -> h.getMetricId().equals(metricId));
    }
}
