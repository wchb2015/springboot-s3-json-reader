package com.example.s3jsonreader.service;

import com.example.s3jsonreader.pojo.POJO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class LocalJsonTest {

    @Test
    public void testConvertLocalJsonToPOJO() throws IOException {
        // Read the local s3.json file
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = new File("src/test/resources/s3.json");
        
        // Parse as JsonNode
        JsonNode rootNode = objectMapper.readTree(jsonFile);
        
        List<POJO> pojoList = new ArrayList<>();
        
        // Process the JSON array
        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                POJO pojo = convertJsonNodeToPOJO(node);
                pojoList.add(pojo);
            }
        }
        
        // Verify the results
        assertNotNull(pojoList, "POJO list should not be null");
        assertEquals(2, pojoList.size(), "POJO list should contain 2 elements");
        
        // Verify first POJO
        POJO firstPojo = pojoList.get(0);
        assertNotNull(firstPojo.getAdIdToPayload(), "First POJO adIdToPayload should not be null");
        assertNotNull(firstPojo.getAdIdToQueries(), "First POJO adIdToQueries should not be null");
        assertEquals(3, firstPojo.getAdIdToPayload().size(), "First POJO should have 3 payload entries");
        assertEquals(3, firstPojo.getAdIdToQueries().size(), "First POJO should have 3 query entries");
        
        // Verify specific values
        assertEquals("payload_content_001", firstPojo.getAdIdToPayload().get("ad_001"));
        assertTrue(firstPojo.getAdIdToQueries().get("ad_001").contains("query_001_a"));
        assertTrue(firstPojo.getAdIdToQueries().get("ad_001").contains("query_001_b"));
        
        // Verify second POJO
        POJO secondPojo = pojoList.get(1);
        assertNotNull(secondPojo.getAdIdToPayload(), "Second POJO adIdToPayload should not be null");
        assertNotNull(secondPojo.getAdIdToQueries(), "Second POJO adIdToQueries should not be null");
        assertEquals(2, secondPojo.getAdIdToPayload().size(), "Second POJO should have 2 payload entries");
        assertEquals(2, secondPojo.getAdIdToQueries().size(), "Second POJO should have 2 query entries");
        
        System.out.println("Successfully converted local s3.json to List<POJO>:");
        System.out.println(pojoList);
    }
    
    private POJO convertJsonNodeToPOJO(JsonNode node) {
        POJO pojo = new POJO();
        
        Map<String, String> adIdToPayload = new HashMap<>();
        Map<String, Set<String>> adIdToQueries = new HashMap<>();
        
        // Parse adIdToPayload
        if (node.has("adIdToPayload") && !node.get("adIdToPayload").isNull()) {
            JsonNode payloadNode = node.get("adIdToPayload");
            if (payloadNode.isObject()) {
                payloadNode.fields().forEachRemaining(entry -> {
                    adIdToPayload.put(entry.getKey(), entry.getValue().asText());
                });
            }
        }
        
        // Parse adIdToQueries
        if (node.has("adIdToQueries") && !node.get("adIdToQueries").isNull()) {
            JsonNode queriesNode = node.get("adIdToQueries");
            if (queriesNode.isObject()) {
                queriesNode.fields().forEachRemaining(entry -> {
                    Set<String> queries = new HashSet<>();
                    JsonNode queryArray = entry.getValue();
                    if (queryArray.isArray()) {
                        queryArray.forEach(q -> queries.add(q.asText()));
                    }
                    adIdToQueries.put(entry.getKey(), queries);
                });
            }
        }
        
        pojo.setAdIdToPayload(adIdToPayload);
        pojo.setAdIdToQueries(adIdToQueries);
        
        return pojo;
    }
}
