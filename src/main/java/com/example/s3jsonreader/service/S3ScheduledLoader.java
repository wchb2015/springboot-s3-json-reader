package com.example.s3jsonreader.service;

import com.example.s3jsonreader.config.SchedulerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class S3ScheduledLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(S3ScheduledLoader.class);
    
    private final S3Service s3Service;
    private final S3DataCacheService cacheService;
    private final SchedulerConfig schedulerConfig;
    private ScheduledExecutorService scheduledExecutorService;
    
    public S3ScheduledLoader(S3Service s3Service, 
                            S3DataCacheService cacheService,
                            SchedulerConfig schedulerConfig) {
        this.s3Service = s3Service;
        this.cacheService = cacheService;
        this.schedulerConfig = schedulerConfig;
    }
    
    /**
     * Initializes the scheduled executor service and starts the background task
     */
    @PostConstruct
    public void init() {
        if (!schedulerConfig.isEnabled()) {
            logger.info("S3 Scheduler is disabled. Skipping initialization.");
            return;
        }
        
        logger.info("Initializing S3 Scheduled Loader with configuration: " +
                   "initialDelay={}ms, fixedDelay={}ms, threadPoolSize={}", 
                   schedulerConfig.getInitialDelay(), 
                   schedulerConfig.getFixedDelay(),
                   schedulerConfig.getThreadPoolSize());
        
        // Create the scheduled executor service
        scheduledExecutorService = Executors.newScheduledThreadPool(
            schedulerConfig.getThreadPoolSize(),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("s3-loader-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }
        );
        
        // Schedule the task
        scheduledExecutorService.scheduleWithFixedDelay(
            this::loadDataFromS3,
            schedulerConfig.getInitialDelay(),
            schedulerConfig.getFixedDelay(),
            TimeUnit.MILLISECONDS
        );
        
        logger.info("S3 Scheduled Loader started successfully");
    }
    
    /**
     * The task that loads data from S3
     */
    private void loadDataFromS3() {
        logger.debug("Starting scheduled S3 data load");
        
        try {
            // Load data from S3
            JsonNode jsonData = s3Service.readJsonFromS3();
            
            // Update the cache
            cacheService.updateCache(jsonData);
            
            logger.info("Successfully loaded and cached data from S3. Cache stats: {}", 
                       cacheService.getStats());
            
        } catch (IOException e) {
            logger.error("Failed to load data from S3: {}", e.getMessage(), e);
            cacheService.recordFailedLoad();
        } catch (Exception e) {
            logger.error("Unexpected error during S3 data load: {}", e.getMessage(), e);
            cacheService.recordFailedLoad();
        }
    }
    
    /**
     * Manually trigger a data load (useful for testing or on-demand refresh)
     * @return true if load was successful, false otherwise
     */
    public boolean triggerManualLoad() {
        logger.info("Manual S3 data load triggered");
        
        try {
            JsonNode jsonData = s3Service.readJsonFromS3();
            cacheService.updateCache(jsonData);
            logger.info("Manual load completed successfully");
            return true;
        } catch (Exception e) {
            logger.error("Manual load failed: {}", e.getMessage(), e);
            cacheService.recordFailedLoad();
            return false;
        }
    }
    
    /**
     * Gracefully shuts down the scheduled executor service
     */
    @PreDestroy
    public void shutdown() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            logger.info("Shutting down S3 Scheduled Loader");
            
            scheduledExecutorService.shutdown();
            
            try {
                // Wait for tasks to complete
                if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warn("Scheduled executor did not terminate in time, forcing shutdown");
                    scheduledExecutorService.shutdownNow();
                    
                    if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.error("Scheduled executor did not terminate after forced shutdown");
                    }
                }
                logger.info("S3 Scheduled Loader shut down successfully");
            } catch (InterruptedException e) {
                logger.error("Interrupted while shutting down scheduled executor", e);
                scheduledExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Check if the scheduler is running
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return scheduledExecutorService != null && 
               !scheduledExecutorService.isShutdown() && 
               !scheduledExecutorService.isTerminated();
    }
    
    /**
     * Get the current cache statistics
     * @return CacheStats object
     */
    public S3DataCacheService.CacheStats getCacheStats() {
        return cacheService.getStats();
    }
}
