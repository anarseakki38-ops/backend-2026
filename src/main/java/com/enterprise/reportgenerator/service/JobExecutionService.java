package com.enterprise.reportgenerator.service;

import com.enterprise.reportgenerator.model.JobConfig;
import com.enterprise.reportgenerator.model.Report;
import com.enterprise.reportgenerator.repository.JsonConfigRepository;
import com.enterprise.reportgenerator.repository.JsonReportRepository;
import com.enterprise.reportgenerator.util.ExcelGenerator;
import com.enterprise.reportgenerator.util.ErrorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {

    @org.springframework.beans.factory.annotation.Qualifier("primaryJdbcTemplate")
    private final JdbcTemplate primaryJdbcTemplate;

    @org.springframework.beans.factory.annotation.Qualifier("secondaryJdbcTemplate")
    private final JdbcTemplate secondaryJdbcTemplate;

    private final JsonConfigRepository jobRepository;
    private final JsonReportRepository reportRepository;
    private final EmailService emailService;
    private final MetricCache metricCache;

    @Value("${app.config.sql-path:data/sql}")
    private String sqlBasePath;

    @Value("${app.config.output-path:data/reports}")
    private String outputBasePath;

    public void executeJob(String jobId) {
        log.info("Starting execution for Job ID: {}", jobId);
        Optional<JobConfig> jobOpt = jobRepository.findById(jobId);

        if (!jobOpt.isPresent()) {
            log.error("Job ID {} not found in configuration.", jobId);
            return;
        }

        JobConfig job = jobOpt.get();
        String reportId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 1. Read SQL
            if (job.getSqlFileName() == null || job.getSqlFileName().isEmpty()) {
                log.error("SQL file name is missing for job: {}", job.getName());
                saveFailedReport(reportId, job, "SQL file name is missing in job configuration");
                return;
            }

            Path sqlPath = Paths.get(sqlBasePath, job.getSqlFileName());
            if (!Files.exists(sqlPath)) {
                log.error("SQL file not found: {}", sqlPath);
                saveFailedReport(reportId, job, "SQL file not found: " + sqlPath);
                return;
            }
            String sql = new String(Files.readAllBytes(sqlPath));

            // Select DataSource
            JdbcTemplate jdbcTemplate = "SECONDARY".equalsIgnoreCase(job.getTargetDatabase())
                    ? secondaryJdbcTemplate
                    : primaryJdbcTemplate;
            String dbType = "SECONDARY".equalsIgnoreCase(job.getTargetDatabase()) ? "SECONDARY" : "PRIMARY";

            // 2. Execute SQL
            log.info("Executing SQL for job {} on {} database", job.getName(), dbType);
            List<Map<String, Object>> results;

            // Check if date parameters are provided
            if (job.getFromDate() != null && job.getToDate() != null &&
                    !job.getFromDate().isEmpty() && !job.getToDate().isEmpty()) {
                // Use NamedParameterJdbcTemplate for parameter binding
                NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
                Map<String, Object> params = new HashMap<>();

                try {
                    // Parse dates from yyyy-MM-dd format
                    LocalDate from = LocalDate.parse(job.getFromDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                    LocalDate to = LocalDate.parse(job.getToDate(), DateTimeFormatter.ISO_LOCAL_DATE);

                    // Convert to java.sql.Date for Oracle
                    params.put("FromDate", java.sql.Date.valueOf(from));
                    params.put("ToDate", java.sql.Date.valueOf(to));

                    log.info("Binding date parameters - FromDate: {}, ToDate: {}", job.getFromDate(), job.getToDate());
                    results = namedTemplate.queryForList(sql, params);
                } catch (Exception e) {
                    log.error("Error parsing or binding date parameters", e);
                    throw new RuntimeException("Invalid date format. Expected: yyyy-MM-dd", e);
                }
            } else {
                // No parameters - execute as normal
                results = jdbcTemplate.queryForList(sql);
            }

            // 3. Generate Excel
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String dayFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            Path outputDir = Paths.get(outputBasePath, dayFolder);
            Files.createDirectories(outputDir);

            String fileName = job.getSqlFileName().replace(".txt", "") + "_" + timestamp + ".xlsx";
            Path outputPath = outputDir.resolve(fileName);

            ExcelGenerator.generateExcel(results, outputPath.toString());

            // 4. Save Report Metadata
            Report report = new Report();
            report.setId(reportId);
            report.setJobId(jobId);
            report.setJobName(job.getName());
            report.setFileName(fileName);
            report.setFilePath(outputPath.toString());
            report.setGeneratedAt(startTime);
            report.setStatus("SUCCESS");
            report.setRowCount(results.size());

            File reportFile = outputPath.toFile();
            report.setFileSizeBytes(reportFile.exists() ? reportFile.length() : 0);

            reportRepository.saveReport(report);
            log.info("Report metadata saved: {}", reportId);

            // 5. Send Email
            if (job.isEmailEnabled()) {
                try {
                    log.info("Attempting to send report email for job {}", job.getName());
                    emailService.sendReportEmail(
                            job.getEmailRecipients(),
                            "Report Generated: " + job.getName(),
                            "Please find attached the report for " + job.getName(),
                            reportFile);
                    log.info("Email sent successfully for job {}", job.getName());
                } catch (Exception e) {
                    log.error("Email delivery failed for job {}: {}", job.getName(), e.getMessage());
                    // Update report status to reflect email failure but general success
                    report.setErrorMessage("Report generated, but email delivery failed.");
                    reportRepository.saveReport(report);
                }
            }

            log.info("Job {} completed successfully. Report ID: {}", job.getName(), reportId);

        } catch (Exception e) {
            log.error("Critical error executing job {}", job.getName(), e);
            String sanitizedMessage = ErrorUtil.sanitizeErrorMessage(e.getMessage());
            saveFailedReport(reportId, job, sanitizedMessage);
        }
    }

    private void saveFailedReport(String reportId, JobConfig job, String errorMessage) {
        Report report = new Report();
        report.setId(reportId);
        report.setJobId(job.getId());
        report.setJobName(job.getName());
        report.setFileName("FAILED");
        report.setFilePath("");
        report.setGeneratedAt(System.currentTimeMillis());
        report.setStatus("FAILED");
        report.setErrorMessage(errorMessage);
        reportRepository.saveReport(report);
        log.warn("Failed report saved: {}", reportId);
    }

    public void resendLastReportEmail(String jobId) {
        log.info("Request to resend last report email for Job ID: {}", jobId);
        Optional<JobConfig> jobOpt = jobRepository.findById(jobId);
        if (!jobOpt.isPresent()) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        JobConfig job = jobOpt.get();

        if (!job.isEmailEnabled() || job.getEmailRecipients() == null || job.getEmailRecipients().isEmpty()) {
            throw new IllegalStateException("Email is not enabled or no recipients configured for this job.");
        }

        List<Report> reports = reportRepository.findByJobId(jobId);
        Optional<Report> lastReportOpt = reports.stream()
                .filter(r -> "SUCCESS".equals(r.getStatus()))
                .sorted((r1, r2) -> Long.compare(r2.getGeneratedAt(), r1.getGeneratedAt())) // Descending
                .findFirst();

        if (!lastReportOpt.isPresent()) {
            throw new IllegalStateException("No successful reports found for this job.");
        }

        Report lastReport = lastReportOpt.get();
        File reportFile = new File(lastReport.getFilePath());

        if (!reportFile.exists()) {
            throw new IllegalStateException("Report file not found on disk: " + lastReport.getFilePath());
        }

        emailService.sendReportEmail(
                job.getEmailRecipients(),
                "RESENT: Report Generated: " + job.getName(),
                "This is a manually triggered resend of the last generated report for " + job.getName()
                        + ".\nGenerated at: "
                        + LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastReport.getGeneratedAt()),
                                java.time.ZoneId.systemDefault()),
                reportFile);

        log.info("Resent email for report: {}", lastReport.getId());
    }
}
