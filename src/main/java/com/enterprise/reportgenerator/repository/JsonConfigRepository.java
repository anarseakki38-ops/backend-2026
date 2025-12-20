package com.enterprise.reportgenerator.repository;

import com.enterprise.reportgenerator.model.JobConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class JsonConfigRepository {

    private final ObjectMapper objectMapper;
    private final String configFilePath;
    private final List<JobConfig> jobsCache = new CopyOnWriteArrayList<>();

    public JsonConfigRepository(ObjectMapper objectMapper,
            @Value("${app.config.jobs-path:config/jobs.json}") String configFilePath) {
        this.objectMapper = objectMapper;
        this.configFilePath = configFilePath;
    }

    @PostConstruct
    public void init() {
        loadConfigs();
    }

    private void loadConfigs() {
        File file = new File(configFilePath);
        if (file.exists()) {
            try {
                List<JobConfig> loadedJobs = objectMapper.readValue(file, new TypeReference<List<JobConfig>>() {
                });
                jobsCache.clear();
                jobsCache.addAll(loadedJobs);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load job configs from " + configFilePath, e);
            }
        } else {
            // Ensure directory exists
            file.getParentFile().mkdirs();
            saveConfigs(); // Create empty file
        }
    }

    private synchronized void saveConfigs() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(configFilePath), jobsCache);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save job configs", e);
        }
    }

    public List<JobConfig> findAll() {
        return new ArrayList<>(jobsCache);
    }

    public Optional<JobConfig> findById(String id) {
        return jobsCache.stream().filter(j -> j.getId().equals(id)).findFirst();
    }

    public void save(JobConfig jobConfig) {
        jobsCache.removeIf(j -> j.getId().equals(jobConfig.getId()));
        jobsCache.add(jobConfig);
        saveConfigs();
    }

    public void deleteById(String id) {
        jobsCache.removeIf(j -> j.getId().equals(id));
        saveConfigs();
    }
}
