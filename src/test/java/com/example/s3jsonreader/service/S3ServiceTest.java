package com.example.s3jsonreader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    private ObjectMapper objectMapper;
    private static final String BUCKET_NAME = "chongbei-test-input";
    private static final String JSON_FILE_KEY = "data.json";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Set the bucket name and file key using reflection
        ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(s3Service, "jsonFileKey", JSON_FILE_KEY);
    }

    @Test
    void testReadJsonFromS3_Success() throws IOException {
        // Arrange
        String jsonContent = """
                {
                    "id": 1,
                    "name": "Test Data",
                    "description": "This is test data from chongbei-test-input bucket",
                    "items": [
                        {"itemId": 1, "itemName": "Item 1"},
                        {"itemId": 2, "itemName": "Item 2"}
                    ],
                    "metadata": {
                        "version": "1.0",
                        "timestamp": "2024-01-01T00:00:00Z"
                    }
                }
                """;

        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("application/json")
                .contentLength((long) jsonContent.length())
                .eTag("test-etag")
                .build();

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(response, AbortableInputStream.create(inputStream));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        // Act
        JsonNode result = s3Service.readJsonFromS3();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.get("id").asInt());
        assertEquals("Test Data", result.get("name").asText());
        assertEquals("This is test data from chongbei-test-input bucket", result.get("description").asText());
        assertTrue(result.has("items"));
        assertTrue(result.get("items").isArray());
        assertEquals(2, result.get("items").size());
        assertEquals("Item 1", result.get("items").get(0).get("itemName").asText());
        assertTrue(result.has("metadata"));
        assertEquals("1.0", result.get("metadata").get("version").asText());

        // Verify the correct bucket and key were used
        verify(s3Client).getObject(argThat((GetObjectRequest request) ->
                BUCKET_NAME.equals(request.bucket()) &&
                        JSON_FILE_KEY.equals(request.key())
        ));
    }

    @Test
    void testReadJsonFromS3ByKey_Success() throws IOException {
        // Arrange
        String customKey = "custom/path/data.json";
        String jsonContent = """
                {
                    "status": "success",
                    "data": {
                        "value": 42,
                        "message": "Custom key test"
                    }
                }
                """;

        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("application/json")
                .contentLength((long) jsonContent.length())
                .build();

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(response, AbortableInputStream.create(inputStream));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        // Act
        JsonNode result = s3Service.readJsonFromS3ByKey(customKey);

        // Assert
        assertNotNull(result);
        assertEquals("success", result.get("status").asText());
        assertTrue(result.has("data"));
        assertEquals(42, result.get("data").get("value").asInt());
        assertEquals("Custom key test", result.get("data").get("message").asText());

        // Verify the correct bucket and custom key were used
        verify(s3Client).getObject(argThat((GetObjectRequest request) ->
                BUCKET_NAME.equals(request.bucket()) &&
                        customKey.equals(request.key())
        ));
    }

    @Test
    void testReadJsonFromS3_S3Exception() {
        // Arrange
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
                .message("Access Denied")
                .statusCode(403)
                .awsErrorDetails(software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                        .errorCode("AccessDenied")
                        .errorMessage("Access Denied to bucket: " + BUCKET_NAME)
                        .build())
                .build();

        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(s3Exception);

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> s3Service.readJsonFromS3());
        assertTrue(exception.getMessage().contains("Failed to read JSON file from S3"));
        assertTrue(exception.getMessage().contains("Access Denied"));
    }

    @Test
    void testReadJsonFromS3_InvalidJson() {
        // Arrange
        String invalidJsonContent = "{ invalid json content }";

        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("application/json")
                .build();

        InputStream inputStream = new ByteArrayInputStream(invalidJsonContent.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(response, AbortableInputStream.create(inputStream));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        // Act & Assert
        assertThrows(IOException.class, () -> s3Service.readJsonFromS3());
    }

    @Test
    void testReadJsonFromS3ByKey_NullKey() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> s3Service.readJsonFromS3ByKey(null)
        );
        assertEquals("S3 key cannot be null or empty", exception.getMessage());
    }

    @Test
    void testReadJsonFromS3ByKey_EmptyKey() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> s3Service.readJsonFromS3ByKey("  ")
        );
        assertEquals("S3 key cannot be null or empty", exception.getMessage());
    }

    @Test
    void testReadJsonFromS3_LargeJsonFile() throws IOException {
        // Arrange - Test with a larger, more complex JSON structure
        String largeJsonContent = """
                {
                    "bucket": "chongbei-test-input",
                    "file": "data.json",
                    "records": [
                        {
                            "id": 1,
                            "type": "user",
                            "attributes": {
                                "name": "John Doe",
                                "email": "john@example.com",
                                "roles": ["admin", "user"]
                            }
                        },
                        {
                            "id": 2,
                            "type": "product",
                            "attributes": {
                                "name": "Product A",
                                "price": 99.99,
                                "categories": ["electronics", "gadgets"]
                            }
                        }
                    ],
                    "pagination": {
                        "page": 1,
                        "totalPages": 10,
                        "totalRecords": 100
                    }
                }
                """;

        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("application/json")
                .contentLength((long) largeJsonContent.length())
                .build();

        InputStream inputStream = new ByteArrayInputStream(largeJsonContent.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(response, AbortableInputStream.create(inputStream));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        // Act
        JsonNode result = s3Service.readJsonFromS3();

        // Assert
        assertNotNull(result);
        assertEquals("chongbei-test-input", result.get("bucket").asText());
        assertEquals("data.json", result.get("file").asText());
        assertTrue(result.has("records"));
        assertTrue(result.get("records").isArray());
        assertEquals(2, result.get("records").size());

        // Verify first record
        JsonNode firstRecord = result.get("records").get(0);
        assertEquals("user", firstRecord.get("type").asText());
        assertEquals("John Doe", firstRecord.get("attributes").get("name").asText());

        // Verify pagination
        assertTrue(result.has("pagination"));
        assertEquals(1, result.get("pagination").get("page").asInt());
        assertEquals(100, result.get("pagination").get("totalRecords").asInt());
    }
}
