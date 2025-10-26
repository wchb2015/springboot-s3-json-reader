package com.example.s3jsonreader.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class S3DataCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3DataCacheService.class);
    
    private final AtomicReference<JsonNode> cachedData = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastUpdateTime = new AtomicReference<>();
    private final AtomicLong successfulLoads = new AtomicLong(0);
    private final AtomicLong failedLoads = new AtomicLong(0);
    
    /**
     * Updates the cached data with new JSON content
     * @param data The new JSON data to cache
     */
    public void updateCache(JsonNode data) {
        cachedData.set(data);
        lastUpdateTime.set(LocalDateTime.now());
        successfulLoads.incrementAndGet();
        logger.debug("Cache updated successfully. Total successful loads: {}", successfulLoads.get());
    }
    
    /**
     * Retrieves the currently cached data
     * @return The cached JSON data, or null if no data has been loaded yet
     */
    public JsonNode getCachedData() {
        return cachedData.get();
    }
    
    /**
     * Gets the timestamp of the last successful cache update
     * @return LocalDateTime of last update, or null if never updated
     */
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime.get();
    }
    
    /**
     * Increments the failed load counter
     */
    public void recordFailedLoad() {
        failedLoads.incrementAndGet();
        logger.debug("Failed load recorded. Total failed loads: {}", failedLoads.get());
    }
    
    /**
     * Gets statistics about the cache
     * @return CacheStats object containing cache statistics
     */
    public CacheStats getStats() {
        return new CacheStats(
            successfulLoads.get(),
            failedLoads.get(),
            lastUpdateTime.get(),
            cachedData.get() != null
        );
    }
    
    /**
     * Clears the cache
     */
    public void clearCache() {
        cachedData.set(null);
        lastUpdateTime.set(null);
        logger.info("Cache cleared");
    }
    
    /**
     * Inner class to hold cache statistics
     */
    @Getter
    @AllArgsConstructor
    @ToString
    public static class CacheStats {
        private final long successfulLoads;
        private final long failedLoads;
        private final LocalDateTime lastUpdateTime;
        private final boolean hasData;
    }
}
