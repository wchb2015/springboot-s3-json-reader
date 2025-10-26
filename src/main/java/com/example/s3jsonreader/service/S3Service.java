package com.example.s3jsonreader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.json-file-key}")
    private String jsonFileKey;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode readJsonFromS3() throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(jsonFileKey)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            return objectMapper.readTree(s3Object);

        } catch (S3Exception e) {
            System.out.println("hahaha B");
            System.out.println(e);
            throw new IOException("Failed to read JSON file from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public JsonNode readJsonFromS3ByKey(String key) throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            return objectMapper.readTree(s3Object);

        } catch (S3Exception e) {
            System.out.println("hahaha A");
            System.out.println(e);
            throw new IOException("Failed to read JSON file from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}
