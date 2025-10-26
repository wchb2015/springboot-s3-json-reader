package com.example.s3jsonreader.controller;

import com.example.s3jsonreader.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class JsonController {

    private static final Logger logger = LoggerFactory.getLogger(JsonController.class);
    
    private final S3Service s3Service;

    public JsonController(S3Service s3Service) {
        this.s3Service = s3Service;
        logger.info("JsonController initialized with S3Service");
    }

    @GetMapping("/json")
    public ResponseEntity<?> getJsonFromS3(@RequestParam(required = false) String key) {
        logger.info("Received request to get JSON from S3. Key parameter: {}", key);
        
        try {
            JsonNode jsonData;
            if (key != null && !key.isEmpty()) {
                logger.debug("Fetching JSON with custom key: {}", key);
                jsonData = s3Service.readJsonFromS3ByKey(key);
            } else {
                logger.debug("Fetching JSON with default key from configuration");
                jsonData = s3Service.readJsonFromS3();
            }
            
            logger.info("Successfully retrieved JSON from S3. Response size: {} bytes", 
                    jsonData.toString().length());
            return ResponseEntity.ok(jsonData);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request - Invalid parameter: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (IOException e) {
            logger.error("Failed to retrieve JSON from S3: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.error("Unexpected error while processing request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"An unexpected error occurred\"}");
        }
    }
}
