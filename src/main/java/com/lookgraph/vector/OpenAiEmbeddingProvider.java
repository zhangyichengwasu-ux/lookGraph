package com.lookgraph.vector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public OpenAiEmbeddingProvider(
            ObjectMapper objectMapper,
            @Value("${lookgraph.embedding.api-key}") String apiKey,
            @Value("${lookgraph.embedding.base-url}") String baseUrl,
            @Value("${lookgraph.embedding.model}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.getFirst();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", texts
            );
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Embedding API 调用失败: " + response.statusCode() + " " + response.body());
            }

            EmbeddingResponse embeddingResponse = objectMapper.readValue(response.body(), EmbeddingResponse.class);
            List<float[]> result = new ArrayList<>();
            for (EmbeddingData data : embeddingResponse.data()) {
                result.add(toFloatArray(data.embedding()));
            }
            return result;
        } catch (Exception e) {
            log.error("Embedding 调用失败", e);
            throw new RuntimeException("Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    private float[] toFloatArray(List<Double> doubles) {
        float[] floats = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            floats[i] = doubles.get(i).floatValue();
        }
        return floats;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResponse(List<EmbeddingData> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingData(int index, List<Double> embedding, @JsonProperty("object") String object) {}
}
