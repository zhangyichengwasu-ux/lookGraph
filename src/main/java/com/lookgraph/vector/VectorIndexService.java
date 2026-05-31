package com.lookgraph.vector;

import com.lookgraph.parser.ParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexService {

    private static final String CODE_SEMANTICS = "code_semantics";
    private static final String PROJECT_OVERVIEW = "project_overview";
    private static final int BATCH_SIZE = 64;

    private final ChromaClient chromaClient;
    private final EmbeddingProvider embeddingProvider;

    public void initCollections() {
        chromaClient.getOrCreateCollection(CODE_SEMANTICS);
        chromaClient.getOrCreateCollection(PROJECT_OVERVIEW);
    }

    public void indexEntities(String projectId, ParseResult result) {
        List<String> ids = new ArrayList<>();
        List<String> docs = new ArrayList<>();
        List<Map<String, Object>> metas = new ArrayList<>();

        result.classes().forEach(c -> {
            if (StringUtils.hasText(c.getComment())) {
                ids.add(c.getClassId());
                docs.add(c.getComment());
                Map<String, Object> meta = new HashMap<>();
                meta.put("entity_id", c.getClassId());
                meta.put("entity_type", "class");
                meta.put("file_path", c.getFilePath());
                meta.put("module_name", c.getModuleId() != null ? c.getModuleId() : "");
                meta.put("project_id", projectId);
                metas.add(meta);
            }
        });

        result.methods().forEach(m -> {
            if (StringUtils.hasText(m.getComment())) {
                ids.add(m.getMethodId());
                docs.add(m.getComment());
                Map<String, Object> meta = new HashMap<>();
                meta.put("entity_id", m.getMethodId());
                meta.put("entity_type", "method");
                meta.put("file_path", "");
                meta.put("module_name", "");
                meta.put("project_id", projectId);
                metas.add(meta);
            }
        });

        if (ids.isEmpty()) {
            log.info("无注释实体需要向量化");
            return;
        }

        indexInBatches(ids, docs, metas);
        log.info("向量化完成，共 {} 条", ids.size());
    }

    public void upsert(String projectId, ParseResult result) {
        indexEntities(projectId, result);
    }

    public void deleteByFile(String filePath) {
        chromaClient.deleteByMetadata(CODE_SEMANTICS, Map.of("file_path", filePath));
    }

    public void indexProjectOverview(String projectId, String summary) {
        float[] embedding = embeddingProvider.embed(summary);
        chromaClient.upsert(PROJECT_OVERVIEW,
                List.of(projectId),
                List.of(embedding),
                List.of(summary),
                List.of(Map.of("project_id", projectId, "update_time", System.currentTimeMillis())));
    }

    public List<VectorDocument> search(String query, int topK, Map<String, Object> filter) {
        float[] embedding = embeddingProvider.embed(query);
        return chromaClient.query(CODE_SEMANTICS, embedding, topK, filter);
    }

    private void indexInBatches(List<String> ids, List<String> docs, List<Map<String, Object>> metas) {
        for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, ids.size());
            List<String> batchDocs = docs.subList(i, end);
            List<float[]> embeddings = embeddingProvider.embedBatch(batchDocs);
            chromaClient.upsert(CODE_SEMANTICS,
                    ids.subList(i, end),
                    embeddings,
                    batchDocs,
                    metas.subList(i, end));
        }
    }
}
