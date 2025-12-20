package com.enterprise.reportgenerator.controller;

import com.enterprise.reportgenerator.model.JobConfig;
import com.enterprise.reportgenerator.repository.JsonConfigRepository;
import com.enterprise.reportgenerator.service.JobExecutionService;
import com.enterprise.reportgenerator.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow Next.js dev server
public class JobController {

    private final JsonConfigRepository jobRepository;
    private final SchedulerService schedulerService;
    private final JobExecutionService jobExecutionService;

    @GetMapping
    public List<JobConfig> getAllJobs() {
        return jobRepository.findAll();
    }

    @org.springframework.beans.factory.annotation.Value("${app.config.sql-path:data/sql}")
    private String sqlBasePath;

    @PostMapping
    public ResponseEntity<JobConfig> createOrUpdateJob(@RequestBody JobConfig job) {
        if (job.getId() == null || job.getId().isEmpty()) {
            job.setId(UUID.randomUUID().toString());
        }

        // Handle SQL Content
        if (job.getSqlContent() != null && !job.getSqlContent().isEmpty()) {
            try {
                String fileName = job.getId() + ".txt";
                java.nio.file.Path path = java.nio.file.Paths.get(sqlBasePath, fileName);
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.write(path, job.getSqlContent().getBytes());
                job.setSqlFileName(fileName);
            } catch (java.io.IOException e) {
                return ResponseEntity.internalServerError().build();
            }
        }

        jobRepository.save(job);
        schedulerService.scheduleJob(job);
        return ResponseEntity.ok(job);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable String id) {
        schedulerService.cancelJob(id);
        jobRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobConfig> updateJob(@PathVariable String id, @RequestBody JobConfig job) {
        if (!jobRepository.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        job.setId(id); // Ensure ID doesn't change

        // Handle SQL Content update
        if (job.getSqlContent() != null && !job.getSqlContent().isEmpty()) {
            try {
                String fileName = job.getId() + ".txt";
                java.nio.file.Path path = java.nio.file.Paths.get(sqlBasePath, fileName);
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.write(path, job.getSqlContent().getBytes());
                job.setSqlFileName(fileName);
            } catch (java.io.IOException e) {
                return ResponseEntity.internalServerError().build();
            }
        }

        // Cancel old schedule and create new one
        schedulerService.cancelJob(id);
        jobRepository.save(job);

        if (job.isEnabled()) {
            schedulerService.scheduleJob(job);
        }

        return ResponseEntity.ok(job);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<JobConfig> toggleJobStatus(@PathVariable String id) {
        return jobRepository.findById(id)
                .map(job -> {
                    job.setEnabled(!job.isEnabled());
                    jobRepository.save(job);

                    if (job.isEnabled()) {
                        schedulerService.scheduleJob(job);
                    } else {
                        schedulerService.cancelJob(id);
                    }

                    return ResponseEntity.ok(job);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Void> executeNow(@PathVariable String id) {
        if (!jobRepository.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        // Run async
        new Thread(() -> jobExecutionService.executeJob(id)).start();
        return ResponseEntity.ok().build();
    }
}
