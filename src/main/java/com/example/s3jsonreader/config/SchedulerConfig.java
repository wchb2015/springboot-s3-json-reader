package com.example.s3jsonreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scheduler.s3")
public class SchedulerConfig {
    
    private boolean enabled = true;
    private long initialDelay = 0;
    private long fixedDelay = 5000; // 5 seconds default
    private int threadPoolSize = 2;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public long getInitialDelay() {
        return initialDelay;
    }
    
    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }
    
    public long getFixedDelay() {
        return fixedDelay;
    }
    
    public void setFixedDelay(long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }
    
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
}
