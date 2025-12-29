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

    @PostMapping("/{id}/email")
    public ResponseEntity<?> resendEmail(@PathVariable String id) {
        try {
            jobExecutionService.resendLastReportEmail(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (RuntimeException e) {
            // Handle the wrapped email exception
            String msg = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                msg = e.getCause().getMessage();
            }
            if (msg.contains("Authentication failed") || msg.contains("535")) {
                msg = "Email Authentication Failed. Please check your application.properties settings.";
            }
            return ResponseEntity.internalServerError().body(java.util.Collections.singletonMap("error", msg));
        }
    }
}
