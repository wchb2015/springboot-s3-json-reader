package com.example.s3jsonreader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.json-file-key}")
    private String jsonFileKey;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
        this.objectMapper = new ObjectMapper();
        logger.info("S3Service initialized with S3Client");
    }

    public JsonNode readJsonFromS3() throws IOException {
        logger.info("Attempting to read JSON from S3 - Bucket: {}, Key: {}", bucketName, jsonFileKey);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(jsonFileKey)
                    .build();
            
            logger.debug("Sending GetObject request to S3");
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            
            GetObjectResponse response = s3Object.response();
            logger.debug("S3 Response - Content Type: {}, Content Length: {}, ETag: {}", 
                    response.contentType(), response.contentLength(), response.eTag());
            
            JsonNode jsonNode = objectMapper.readTree(s3Object);
            logger.info("Successfully read and parsed JSON from S3 - Bucket: {}, Key: {}", bucketName, jsonFileKey);
            
            return jsonNode;

        } catch (S3Exception e) {
            logger.error("S3 Exception occurred while reading from bucket: {}, key: {} - Error Code: {}, Status Code: {}, Message: {}", 
                    bucketName, jsonFileKey, e.awsErrorDetails().errorCode(), 
                    e.statusCode(), e.awsErrorDetails().errorMessage(), e);
            throw new IOException("Failed to read JSON file from S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (IOException e) {
            logger.error("IOException occurred while parsing JSON from S3 - Bucket: {}, Key: {}", 
                    bucketName, jsonFileKey, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred while reading from S3 - Bucket: {}, Key: {}", 
                    bucketName, jsonFileKey, e);
            throw new IOException("Unexpected error reading from S3", e);
        }
    }

    public JsonNode readJsonFromS3ByKey(String key) throws IOException {
        logger.info("Attempting to read JSON from S3 with custom key - Bucket: {}, Key: {}", bucketName, key);
        
        if (key == null || key.trim().isEmpty()) {
            logger.error("Invalid key provided: null or empty");
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            logger.debug("Sending GetObject request to S3 for custom key: {}", key);
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            
            GetObjectResponse response = s3Object.response();
            logger.debug("S3 Response for custom key - Content Type: {}, Content Length: {}, ETag: {}", 
                    response.contentType(), response.contentLength(), response.eTag());
            
            JsonNode jsonNode = objectMapper.readTree(s3Object);
            logger.info("Successfully read and parsed JSON from S3 with custom key - Bucket: {}, Key: {}", bucketName, key);
            
            return jsonNode;

        } catch (S3Exception e) {
            logger.error("S3 Exception occurred while reading with custom key - Bucket: {}, Key: {} - Error Code: {}, Status Code: {}, Message: {}", 
                    bucketName, key, e.awsErrorDetails().errorCode(), 
                    e.statusCode(), e.awsErrorDetails().errorMessage(), e);
            throw new IOException("Failed to read JSON file from S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (IOException e) {
            logger.error("IOException occurred while parsing JSON from S3 with custom key - Bucket: {}, Key: {}", 
                    bucketName, key, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred while reading from S3 with custom key - Bucket: {}, Key: {}", 
                    bucketName, key, e);
            throw new IOException("Unexpected error reading from S3", e);
        }
    }
}
