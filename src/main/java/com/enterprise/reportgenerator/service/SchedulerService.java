package com.enterprise.reportgenerator.service;

import com.enterprise.reportgenerator.model.JobConfig;
import com.enterprise.reportgenerator.repository.JsonConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final TaskScheduler taskScheduler;
    private final JsonConfigRepository jobRepository;
    private final JobExecutionService jobExecutionService;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void scheduleAllInfo() {
        log.info("Initializing scheduler...");
        jobRepository.findAll().forEach(this::scheduleJob);
    }

    public void scheduleJob(JobConfig job) {
        cancelJob(job.getId()); // Cancel existing if any

        if (job.getCronExpression() != null && !job.getCronExpression().isEmpty()) {
            try {
                ScheduledFuture<?> future = taskScheduler.schedule(
                        () -> executeJob(job.getId()),
                        new CronTrigger(job.getCronExpression()));
                scheduledTasks.put(job.getId(), future);
                log.info("Scheduled job {} with cron {}", job.getName(), job.getCronExpression());
            } catch (Exception e) {
                log.error("Failed to schedule job {}", job.getName(), e);
            }
        }
    }

    public void cancelJob(String jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled job {}", jobId);
        }
    }

    private void executeJob(String jobId) {
        jobExecutionService.executeJob(jobId);
    }

    public boolean isJobScheduled(String jobId) {
        return scheduledTasks.containsKey(jobId);
    }
}
