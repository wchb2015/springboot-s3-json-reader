package com.example.s3jsonreader.controller;

import com.example.s3jsonreader.service.S3DataCacheService;
import com.example.s3jsonreader.service.S3ScheduledLoader;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerController.class);

    private final S3ScheduledLoader scheduledLoader;
    private final S3DataCacheService cacheService;

    public SchedulerController(S3ScheduledLoader scheduledLoader,
                               S3DataCacheService cacheService) {
        this.scheduledLoader = scheduledLoader;
        this.cacheService = cacheService;
    }

    /**
     * Get the current status of the scheduler and cache
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        logger.info("Getting scheduler status");

        Map<String, Object> status = new HashMap<>();
        S3DataCacheService.CacheStats stats = cacheService.getStats();

        status.put("schedulerRunning", scheduledLoader.isRunning());
        status.put("successfulLoads", stats.getSuccessfulLoads());
        status.put("failedLoads", stats.getFailedLoads());
        status.put("lastUpdateTime", stats.getLastUpdateTime());
        status.put("hasData", stats.isHasData());

        return ResponseEntity.ok(status);
    }

    /**
     * Get the cached data
     */
    @GetMapping("/cached-data")
    public ResponseEntity<?> getCachedData() {
        logger.info("Retrieving cached data");

        JsonNode cachedData = cacheService.getCachedData();

        if (cachedData == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "No data available in cache. The scheduler may still be loading initial data.");
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
        }

        return ResponseEntity.ok(cachedData);
    }

    /**
     * Manually trigger a data load from S3
     */
    @PostMapping("/trigger-load")
    public ResponseEntity<Map<String, Object>> triggerLoad() {
        logger.info("Manual load triggered via REST endpoint");

        boolean success = scheduledLoader.triggerManualLoad();

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("timestamp", LocalDateTime.now());

        if (success) {
            response.put("message", "Data loaded successfully");
            S3DataCacheService.CacheStats stats = cacheService.getStats();
            response.put("stats", Map.of(
                    "successfulLoads", stats.getSuccessfulLoads(),
                    "failedLoads", stats.getFailedLoads(),
                    "lastUpdateTime", stats.getLastUpdateTime()
            ));
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Failed to load data from S3");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Clear the cache
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        logger.info("Clearing cache via REST endpoint");

        cacheService.clearCache();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache cleared successfully");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for the scheduler
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();

        boolean isHealthy = scheduledLoader.isRunning();
        S3DataCacheService.CacheStats stats = cacheService.getStats();

        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("schedulerRunning", isHealthy);

        // Consider unhealthy if no successful loads and has failed loads
        if (stats.getSuccessfulLoads() == 0 && stats.getFailedLoads() > 0) {
            health.put("status", "DEGRADED");
            health.put("warning", "No successful loads, only failures detected");
        }

        // Add last update time if available
        if (stats.getLastUpdateTime() != null) {
            health.put("lastSuccessfulUpdate", stats.getLastUpdateTime());
        }

        HttpStatus status = "UP".equals(health.get("status")) ? HttpStatus.OK :
                "DEGRADED".equals(health.get("status")) ? HttpStatus.OK :
                        HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(health);
    }
}
