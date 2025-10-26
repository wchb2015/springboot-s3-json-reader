package com.example.s3jsonreader.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scheduler.s3")
public class SchedulerConfig {

    private boolean enabled = true;
    private long initialDelay = 0;
    private long fixedDelay = 30; // 30 seconds default
    private int threadPoolSize = 2;
}
