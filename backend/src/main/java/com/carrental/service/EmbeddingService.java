package com.carrental.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 向量化服务 —— 调用 OpenAI Embeddings API 将文本转为向量
 * 用于 RAG 知识库的相似案例检索
 */
@Service
public class EmbeddingService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient httpClient;

    // 向量维度（text-embedding-ada-002 = 1536, text-embedding-3-small = 1536）
    public static final int EMBEDDING_DIM = 1536;

    @PostConstruct
    public void init() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 判断 API Key 是否可用
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.equals("sk-your-api-key-here") && !apiKey.isBlank();
    }

    /**
     * 将单条文本转为向量
     */
    public float[] embed(String text) throws IOException {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? new float[EMBEDDING_DIM] : results.get(0);
    }

    /**
     * 批量文本向量化（减少 API 调用次数）
     */
    public List<float[]> embedBatch(List<String> texts) throws IOException {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "text-embedding-ada-002");
        requestBody.put("input", texts);

        String json = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(baseUrl + "/embeddings")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Embedding API call failed: " + response.code() + " " + response.message());
            }
            String body = response.body().string();
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");

            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode vector = item.path("embedding");
                float[] arr = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    arr[i] = (float) vector.get(i).asDouble();
                }
                embeddings.add(arr);
            }
            return embeddings;
        }
    }

    /**
     * 计算两个向量的余弦相似度
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
