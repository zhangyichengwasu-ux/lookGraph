package com.lookgraph.vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Slf4j
@Component
public class ChromaClient {

    private static final String TENANT = "default_tenant";
    private static final String DATABASE = "default_database";

    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ChromaClient(ObjectMapper objectMapper,
                        @Value("${lookgraph.chroma.url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    private HttpClient http() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    private String collectionPath() {
        return "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections";
    }

    public void getOrCreateCollection(String collectionName) {
        try {
            Map<String, Object> body = Map.of(
                    "name", collectionName,
                    "get_or_create", true
            );
            post(collectionPath(), body);
        } catch (Exception e) {
            log.error("创建集合失败: {}", collectionName, e);
            throw new RuntimeException("ChromaDB getOrCreateCollection 失败: " + e.getMessage(), e);
        }
    }

    public void upsert(String collectionName, List<String> ids, List<float[]> embeddings,
                       List<String> documents, List<Map<String, Object>> metadatas) {
        try {
            String collectionId = getCollectionId(collectionName);
            List<List<Float>> embeddingList = embeddings.stream()
                    .map(this::toFloatList)
                    .toList();

            Map<String, Object> body = new HashMap<>();
            body.put("ids", ids);
            body.put("embeddings", embeddingList);
            body.put("documents", documents);
            body.put("metadatas", metadatas);

            post(collectionPath() + "/" + collectionId + "/upsert", body);
        } catch (Exception e) {
            log.error("ChromaDB upsert 失败", e);
            throw new RuntimeException("ChromaDB upsert 失败: " + e.getMessage(), e);
        }
    }

    public List<VectorDocument> query(String collectionName, float[] queryEmbedding,
                                      int topK, Map<String, Object> filter) {
        try {
            String collectionId = getCollectionId(collectionName);

            Map<String, Object> body = new HashMap<>();
            body.put("query_embeddings", List.of(toFloatList(queryEmbedding)));
            body.put("n_results", topK);
            body.put("include", List.of("documents", "metadatas", "distances"));
            if (filter != null && !filter.isEmpty()) {
                body.put("where", filter);
            }

            String response = post(collectionPath() + "/" + collectionId + "/query", body);
            return parseQueryResponse(response);
        } catch (Exception e) {
            log.error("ChromaDB query 失败", e);
            return List.of();
        }
    }

    public void deleteByMetadata(String collectionName, Map<String, Object> filter) {
        try {
            String collectionId = getCollectionId(collectionName);
            Map<String, Object> body = Map.of("where", filter);
            post(collectionPath() + "/" + collectionId + "/delete", body);
        } catch (Exception e) {
            log.error("ChromaDB delete 失败", e);
        }
    }

    private String getCollectionId(String collectionName) throws Exception {
        String response;
        try {
            response = get(collectionPath() + "/" + collectionName);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.info("集合不存在，自动创建: {}", collectionName);
                getOrCreateCollection(collectionName);
                response = get(collectionPath() + "/" + collectionName);
            } else {
                throw e;
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(response, Map.class);
        return (String) map.get("id");
    }

    @SuppressWarnings("unchecked")
    private List<VectorDocument> parseQueryResponse(String response) throws Exception {
        Map<String, Object> map = objectMapper.readValue(response, Map.class);
        List<List<String>> ids = (List<List<String>>) map.get("ids");
        List<List<String>> documents = (List<List<String>>) map.get("documents");
        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) map.get("metadatas");
        List<List<Double>> distances = (List<List<Double>>) map.get("distances");

        if (ids == null || ids.isEmpty() || ids.getFirst().isEmpty()) {
            return List.of();
        }

        List<VectorDocument> results = new ArrayList<>();
        List<String> firstIds = ids.getFirst();
        for (int i = 0; i < firstIds.size(); i++) {
            VectorDocument doc = new VectorDocument();
            doc.setId(firstIds.get(i));
            doc.setDocument(documents != null ? documents.getFirst().get(i) : "");
            doc.setMetadata(metadatas != null ? metadatas.getFirst().get(i) : Map.of());
            doc.setScore(distances != null ? 1.0 - distances.getFirst().get(i) : 0.0);
            results.add(doc);
        }
        return results;
    }

    private String post(String path, Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = http().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("ChromaDB 请求失败: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        HttpResponse<String> response = http().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("ChromaDB 请求失败: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) {
            list.add(f);
        }
        return list;
    }
}
