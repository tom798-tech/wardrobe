package com.tom.wardrobe.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Mock Embedding 控制器 - 用于本地测试
 * 模拟 OpenAI Embedding API 响应格式
 */
@RestController
@RequestMapping("/api/mock-embedding")
public class MockEmbeddingController {

    private static final Random random = new Random(42); // 固定种子保证可复现

    /**
     * 模拟 OpenAI Embedding API
     * POST /api/mock-embedding/v1/embeddings
     */
    @PostMapping("/v1/embeddings")
    public Map<String, Object> createEmbedding(@RequestBody Map<String, Object> request) {
        String input = (String) request.get("input");
        String model = (String) request.getOrDefault("model", "text-embedding-3-small");

        List<Double> embedding = generateRandomEmbedding(input, 1536);

        Map<String, Object> response = new HashMap<>();
        response.put("object", "list");

        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("object", "embedding");
        item.put("embedding", embedding);
        item.put("index", 0);
        data.add(item);
        response.put("data", data);

        Map<String, Object> usage = new HashMap<>();
        usage.put("prompt_tokens", input != null ? input.length() / 4 : 0);
        usage.put("total_tokens", input != null ? input.length() / 4 : 0);
        response.put("usage", usage);

        return response;
    }

    /**
     * 根据输入文本生成伪随机向量（相同输入产生相同向量）
     */
    private List<Double> generateRandomEmbedding(String input, int dimensions) {
        List<Double> embedding = new ArrayList<>(dimensions);
        
        // 使用输入作为种子的一部分，保证相同输入产生相同向量
        int seed = input != null ? input.hashCode() : 0;
        Random seededRandom = new Random(seed);

        for (int i = 0; i < dimensions; i++) {
            // 生成 -1 到 1 之间的随机数，模拟真实 embedding
            double value = seededRandom.nextDouble() * 2 - 1;
            embedding.add(Math.round(value * 10000.0) / 10000.0); // 保留4位小数
        }

        return embedding;
    }
}