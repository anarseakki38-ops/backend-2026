package com.enterprise.reportgenerator.repository;

import com.enterprise.reportgenerator.model.Report;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class JsonReportRepository {

    private final ObjectMapper objectMapper;
    private final String reportsFilePath;

    public JsonReportRepository(ObjectMapper objectMapper,
            @Value("${app.config.data-path:data}/reports.json") String reportsFilePath) {
        this.objectMapper = objectMapper;
        this.reportsFilePath = reportsFilePath;
    }

    @PostConstruct
    public void init() {
        File file = new File(reportsFilePath);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                save(new ArrayList<>());
            } catch (IOException e) {
                log.error("Failed to create reports.json", e);
            }
        }
    }

    private List<Report> load() {
        try {
            File file = new File(reportsFilePath);
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<Report>>() {
            });
        } catch (Exception e) {
            log.error("Failed to load reports", e);
            return new ArrayList<>();
        }
    }

    private void save(List<Report> reports) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(reportsFilePath), reports);
        } catch (IOException e) {
            log.error("Failed to save reports", e);
        }
    }

    public void saveReport(Report report) {
        List<Report> reports = load();
        reports.removeIf(r -> r.getId().equals(report.getId()));
        reports.add(report);
        save(reports);
        log.info("Report saved: {}", report.getId());
    }

    public List<Report> findAll() {
        return load();
    }

    public Optional<Report> findById(String id) {
        return load().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    public List<Report> findByJobId(String jobId) {
        return load().stream()
                .filter(r -> r.getJobId().equals(jobId))
                .collect(Collectors.toList());
    }

    public void deleteById(String id) {
        List<Report> reports = load();
        reports.removeIf(r -> r.getId().equals(id));
        save(reports);
        log.info("Report deleted: {}", id);
    }
}
