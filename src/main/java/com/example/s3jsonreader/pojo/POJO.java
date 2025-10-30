package com.example.s3jsonreader.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@ToString
public class POJO {
    private Map<String, String> adIdToPayload;
    private Map<String, Set<String>> adIdToQueries;
}
