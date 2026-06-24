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
            ids.add(c.getClassId());
            docs.add(buildClassIndexText(c));
            Map<String, Object> meta = new HashMap<>();
            meta.put("entity_id", c.getClassId());
            meta.put("entity_type", "class");
            meta.put("file_path", c.getFilePath());
            meta.put("module_name", c.getModuleId() != null ? c.getModuleId() : "");
            meta.put("project_id", projectId);
            metas.add(meta);
        });

        result.methods().forEach(m -> {
            ids.add(m.getMethodId());
            docs.add(buildMethodIndexText(m));
            Map<String, Object> meta = new HashMap<>();
            meta.put("entity_id", m.getMethodId());
            meta.put("entity_type", "method");
            meta.put("file_path", "");
            meta.put("module_name", "");
            meta.put("project_id", projectId);
            metas.add(meta);
        });

        if (ids.isEmpty()) {
            log.info("无实体需要向量化");
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

    public void indexSingleSemantic(String entityId, String entityType, String content, String projectId) {
        String indexText = buildIndexTextFromId(entityId, entityType, content);

        float[] embedding = embeddingProvider.embed(indexText);
        Map<String, Object> meta = new HashMap<>();
        meta.put("entity_id", entityId);
        meta.put("entity_type", entityType.toLowerCase());
        meta.put("file_path", "");
        meta.put("module_name", "");
        meta.put("project_id", projectId);

        chromaClient.upsert(CODE_SEMANTICS,
                List.of(entityId),
                List.of(embedding),
                List.of(indexText),
                List.of(meta));

        log.info("单条语义注释向量化完成: entityId={}, type={}", entityId, entityType);
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

    /**
     * 构建类的索引文本：类名 | 驼峰拆词 | 全限定名 | 注释
     */
    private String buildClassIndexText(com.lookgraph.domain.node.ClassNode classNode) {
        StringBuilder sb = new StringBuilder();

        String simpleName = classNode.getName();
        String fullName = classNode.getClassId();
        String comment = classNode.getComment();

        // 简单类名
        if (StringUtils.hasText(simpleName)) {
            sb.append(simpleName).append(" | ");
        }

        // 驼峰拆词
        if (StringUtils.hasText(simpleName)) {
            sb.append(splitCamelCase(simpleName)).append(" | ");
        }

        // 全限定名
        if (StringUtils.hasText(fullName)) {
            sb.append(fullName).append(" | ");
        }

        // 注释（如果有，重复一次以增加权重）
        if (StringUtils.hasText(comment)) {
            sb.append(comment).append(" | ").append(comment);
        }

        return sb.toString();
    }

    /**
     * 构建方法的索引文本：方法名 | 驼峰拆词 | 类名.方法名 | 完整签名 | 注释
     */
    private String buildMethodIndexText(com.lookgraph.domain.node.MethodNode methodNode) {
        StringBuilder sb = new StringBuilder();

        String methodName = methodNode.getName();
        String methodId = methodNode.getMethodId();
        String classId = methodNode.getClassId();
        String comment = methodNode.getComment();

        // 方法名
        if (StringUtils.hasText(methodName)) {
            sb.append(methodName).append(" | ");
        }

        // 驼峰拆词
        if (StringUtils.hasText(methodName)) {
            sb.append(splitCamelCase(methodName)).append(" | ");
        }

        // 类名.方法名
        if (StringUtils.hasText(classId) && StringUtils.hasText(methodName)) {
            String simpleClassName = classId.substring(classId.lastIndexOf('.') + 1);
            sb.append(simpleClassName).append(".").append(methodName).append(" | ");
        }

        // 完整签名
        if (StringUtils.hasText(methodId)) {
            sb.append(methodId).append(" | ");
        }

        // 注释（如果有，重复一次以增加权重）
        if (StringUtils.hasText(comment)) {
            sb.append(comment).append(" | ").append(comment);
        }

        return sb.toString();
    }

    /**
     * 从 entityId 构建索引文本（用于 semantic_annotate）
     */
    private String buildIndexTextFromId(String entityId, String entityType, String content) {
        StringBuilder sb = new StringBuilder();

        if ("class".equalsIgnoreCase(entityType)) {
            // entityId 就是全限定类名：com.example.PriceChannelHelper
            String simpleName = entityId.substring(entityId.lastIndexOf('.') + 1);

            sb.append(simpleName).append(" | ");
            sb.append(splitCamelCase(simpleName)).append(" | ");
            sb.append(entityId).append(" | ");

        } else if ("method".equalsIgnoreCase(entityType)) {
            // entityId 格式：com.example.Class#method(params)
            int hashIndex = entityId.indexOf('#');
            if (hashIndex > 0) {
                String classId = entityId.substring(0, hashIndex);
                String methodSig = entityId.substring(hashIndex + 1);
                int parenIndex = methodSig.indexOf('(');
                String methodName = parenIndex > 0 ? methodSig.substring(0, parenIndex) : methodSig;
                String simpleClassName = classId.substring(classId.lastIndexOf('.') + 1);

                sb.append(methodName).append(" | ");
                sb.append(splitCamelCase(methodName)).append(" | ");
                sb.append(simpleClassName).append(".").append(methodName).append(" | ");
                sb.append(entityId).append(" | ");
            }
        }

        // 注释（如果有，重复一次以增加权重）
        if (StringUtils.hasText(content)) {
            sb.append(content).append(" | ").append(content);
        }

        return sb.toString();
    }

    /**
     * 驼峰命名拆词：PriceChannelHelper -> Price Channel Helper
     */
    private String splitCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replaceAll("([a-z])([A-Z])", "$1 $2")
                    .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
    }
}
