package com.enterprise.reportgenerator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    @Value("${app.datasource.primary.name:Primary Database}")
    private String primaryDbName;

    @Value("${app.datasource.secondary.name:Secondary Database}")
    private String secondaryDbName;

    @GetMapping("/databases")
    public ResponseEntity<Map<String, String>> getDatabaseNames() {
        Map<String, String> dbNames = new HashMap<>();
        dbNames.put("PRIMARY", primaryDbName);
        dbNames.put("SECONDARY", secondaryDbName);
        return ResponseEntity.ok(dbNames);
    }
}
