package com.example.s3jsonreader.controller;

import com.example.s3jsonreader.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
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

    private final S3Service s3Service;

    public JsonController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/json")
    public ResponseEntity<?> getJsonFromS3(@RequestParam(required = false) String key) {
        try {
            JsonNode jsonData;
            if (key != null && !key.isEmpty()) {
                jsonData = s3Service.readJsonFromS3ByKey(key);
            } else {
                jsonData = s3Service.readJsonFromS3();
            }
            return ResponseEntity.ok(jsonData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
