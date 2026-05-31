package com.lookgraph.dto.response;

import com.lookgraph.vector.VectorDocument;

import java.util.Map;

public record SemanticHit(
        double score,
        String document,
        Map<String, Object> metadata,
        Object entity
) {
    public static SemanticHit from(VectorDocument doc, Object entity) {
        return new SemanticHit(doc.getScore(), doc.getDocument(), doc.getMetadata(), entity);
    }
}
