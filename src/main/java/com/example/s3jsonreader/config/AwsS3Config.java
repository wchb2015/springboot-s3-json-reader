package com.example.s3jsonreader.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

    private static final Logger logger = LoggerFactory.getLogger(AwsS3Config.class);

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        logger.info("Initializing S3Client with region: {}", region);
        
        try {
            // Use DefaultCredentialsProvider which automatically checks:
            // 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)
            // 2. System properties
            // 3. Web Identity Token from AWS STS
            // 4. Credentials file at ~/.aws/credentials
            // 5. EC2 Instance profile credentials
            DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
            logger.debug("Using DefaultCredentialsProvider for AWS authentication");
            
            S3Client client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
            
            logger.info("S3Client successfully initialized for region: {}", region);
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to initialize S3Client for region: {} - Error: {}", region, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize S3Client", e);
        }
    }
}
