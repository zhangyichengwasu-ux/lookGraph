package com.lookgraph.service;

import com.lookgraph.domain.repository.neo4j.ClassRepository;
import com.lookgraph.domain.repository.neo4j.MethodRepository;
import com.lookgraph.dto.request.SemanticSearchRequest;
import com.lookgraph.dto.response.SemanticHit;
import com.lookgraph.vector.VectorDocument;
import com.lookgraph.vector.VectorIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final VectorIndexService vectorIndexService;
    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;

    public List<SemanticHit> search(SemanticSearchRequest req) {
        Map<String, Object> filter = new HashMap<>();
        if (StringUtils.hasText(req.module())) {
            filter.put("module_name", req.module());
        }
        if (StringUtils.hasText(req.businessTag())) {
            filter.put("business_tag", req.businessTag());
        }

        List<VectorDocument> docs = vectorIndexService.search(req.query(), req.topK(), filter);

        return docs.stream()
                .map(this::enrichWithEntity)
                .toList();
    }

    private SemanticHit enrichWithEntity(VectorDocument doc) {
        String entityType = (String) doc.getMetadata().get("entity_type");
        String entityId = (String) doc.getMetadata().get("entity_id");

        Object entity = null;
        if ("method".equals(entityType)) {
            entity = methodRepository.findById(entityId).orElse(null);
        } else if ("class".equals(entityType)) {
            entity = classRepository.findById(entityId).orElse(null);
        }

        return SemanticHit.from(doc, entity);
    }
}
