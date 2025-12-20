package com.enterprise.reportgenerator.controller;

import com.enterprise.reportgenerator.model.Report;
import com.enterprise.reportgenerator.repository.JsonReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ReportController {

    private final JsonReportRepository reportRepository;

    @GetMapping
    public List<Report> getAllReports(
            @RequestParam(required = false) Long startDate,
            @RequestParam(required = false) Long endDate) {
        List<Report> reports = reportRepository.findAll();

        // Filter by date range if provided
        if (startDate != null) {
            reports = reports.stream()
                    .filter(r -> r.getGeneratedAt() >= startDate)
                    .collect(Collectors.toList());
        }
        if (endDate != null) {
            reports = reports.stream()
                    .filter(r -> r.getGeneratedAt() <= endDate)
                    .collect(Collectors.toList());
        }

        // Sort by generatedAt DESC (newest first)
        return reports.stream()
                .sorted(Comparator.comparingLong(Report::getGeneratedAt).reversed())
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReportById(@PathVariable String id) {
        return reportRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadReport(@PathVariable String id) {
        return reportRepository.findById(id)
                .map(report -> {
                    File file = new File(report.getFilePath());
                    if (!file.exists()) {
                        log.error("Report file not found: {}", report.getFilePath());
                        return ResponseEntity.notFound().<Resource>build();
                    }

                    Resource resource = new FileSystemResource(file);

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + report.getFileName() + "\"")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable String id) {
        return reportRepository.findById(id)
                .map(report -> {
                    // Delete file
                    File file = new File(report.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }

                    // Delete record
                    reportRepository.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
