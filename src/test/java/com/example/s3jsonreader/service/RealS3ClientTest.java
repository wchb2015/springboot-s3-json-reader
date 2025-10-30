package com.example.s3jsonreader.service;

import com.example.s3jsonreader.pojo.POJO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RealS3ClientTest {

    @Test
    public void testReadS3() {
        // 1. Initialize a real S3 client with ~/.aws/credentials
        // Using the default profile from ~/.aws/credentials
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();

        S3Client s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1) // Changed to US_EAST_1 which is the default region
                .build();

        // 2. Read the data.json under chongbei-test-input bucket
        String bucketName = "chongbei-test-input";
        String key = "data.json";

        try {
            System.out.println("Attempting to read file from S3...");
            System.out.println("Bucket: " + bucketName);
            System.out.println("Key: " + key);
            System.out.println("----------------------------------------");

            // Create a GetObjectRequest
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Get the S3 object
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            // Read the content from the S3 object
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s3Object, StandardCharsets.UTF_8))) {

                // 3. Print the content of this file
                String content = reader.lines()
                        .collect(Collectors.joining("\n"));

                System.out.println("File Content:");
                System.out.println(content);
                System.out.println("----------------------------------------");
                System.out.println("Successfully read file from S3!");

            }

        } catch (Exception e) {
            System.err.println("Error reading file from S3: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to read from S3", e);
        } finally {
            // Close the S3 client
            s3Client.close();
        }
    }

    @Test
    public void convertToPOJO() throws IOException {
        List<POJO> pojoList = covertToPOJO();
        System.out.println(pojoList);

        // Assert that the list is not null
        assertNotNull(pojoList, "POJO list should not be null");

        // Assert that the list has size 2
        assertEquals(2, pojoList.size(), "POJO list should contain 2 elements");

        // Assert that the POJO fields are not null
        for (POJO pojo : pojoList) {
            assertNotNull(pojo.getAdIdToPayload(), "adIdToPayload should not be null");
            assertNotNull(pojo.getAdIdToQueries(), "adIdToQueries should not be null");
            assertFalse(pojo.getAdIdToPayload().isEmpty(), "adIdToPayload should not be empty");
            assertFalse(pojo.getAdIdToQueries().isEmpty(), "adIdToQueries should not be empty");
        }

        System.out.println("Successfully converted s3.json to List<POJO> with " + pojoList.size() + " elements");
    }

    private List<POJO> covertToPOJO() throws IOException {
        // Initialize S3 client with credentials
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();

        S3Client s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();

        // S3 bucket and file details
        String bucketName = "chongbei-test-input";
        String key = "s3.json";  // Changed from data.json to s3.json as per requirement

        try {
            System.out.println("Loading s3.json from bucket: " + bucketName);

            // Create GetObjectRequest
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Get the S3 object
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            // Read the content and convert to JsonNode first
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(s3Object);

            List<POJO> pojoList = new ArrayList<>();

            // Check if the root is an array
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    POJO pojo = convertJsonNodeToPOJO(node);
                    pojoList.add(pojo);
                }
            }

            System.out.println("Successfully loaded and converted s3.json using JsonNode");
            return pojoList;

        } catch (Exception e) {
            System.err.println("Error reading s3.json from S3: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to read s3.json from S3", e);
        } finally {
            // Close the S3 client
            s3Client.close();
        }
    }

    private POJO convertJsonNodeToPOJO(JsonNode node) {
        POJO pojo = new POJO();
        JsonNode adId = node.get("adId");
        // Initialize the maps
        Map<String, String> adIdToPayload = new HashMap<>();
        Map<String, Set<String>> adIdToQueries = new HashMap<>();

        Set<String> queries = new HashSet<>();
        for (JsonNode q : node.get("queries")) {
            queries.add(q.asText());
        }

        adIdToPayload.put(adId.toString(), node.get("payload").toString());
        adIdToQueries.put(adId.toString(), queries);

        pojo.setAdIdToPayload(adIdToPayload);
        pojo.setAdIdToQueries(adIdToQueries);


        return pojo;
    }
}
