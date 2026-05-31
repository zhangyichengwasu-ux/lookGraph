package com.lookgraph.vector;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VectorDocument {
    private String id;
    private String document;
    private Map<String, Object> metadata;
    private double score;
}
