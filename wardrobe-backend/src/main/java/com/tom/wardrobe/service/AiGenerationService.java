package com.tom.wardrobe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 生成服务
 * 提供商品描述生成、评论摘要等 AI 功能
 */
@Slf4j
@Service
public class AiGenerationService {

    @Resource
    private ObjectMapper objectMapper;

    @Value("${spring.ai.openai.base-url:http://mock-embedding:3000}")
    private String aiBaseUrl;

    @Value("${spring.ai.openai.api-key:sk-demo-key-123456}")
    private String aiApiKey;

    /**
     * 生成商品描述
     *
     * @param clothName 商品名称
     * @param typeName 分类名称
     * @param style    风格
     * @param brand    品牌
     * @return 生成的商品描述
     */
    public String generateDescription(String clothName, String typeName, String style, String brand) {
        try {
            String prompt = String.format("""
                你是一个专业的服装商品文案编辑。请为以下商品生成一段吸引人的商品描述，要求语言生动、专业，突出商品特点和卖点，字数控制在80-150字之间，适合电商平台展示。
                
                商品名称：%s
                分类：%s
                风格：%s
                品牌：%s
                """, clothName, 
                typeName != null ? typeName : "", 
                style != null ? style : "", 
                brand != null ? brand : "");

            String response = callChatApi(prompt);
            log.info("AI生成商品描述成功，商品名称: {}", clothName);
            return response;
        } catch (Exception e) {
            log.error("AI生成商品描述失败", e);
            return "这款" + clothName + "是本季爆款单品！精选优质面料，舒适透气，经典版型百搭不挑人。时尚设计，彰显个性魅力，穿上它让你瞬间成为焦点！无论是日常通勤还是周末约会，都是你的绝佳选择。";
        }
    }

    /**
     * 生成商品评论摘要（模拟数据）
     *
     * @param clothId 商品ID
     * @return 评论摘要
     */
    public Map<String, Object> generateCommentSummary(Integer clothId) {
        Map<String, Object> result = new HashMap<>();
        result.put("positivePoints", List.of("质量不错", "款式新颖", "性价比高"));
        result.put("negativePoints", List.of("暂无差评"));
        result.put("summary", "该商品整体评价良好，用户反馈积极。");
        return result;
    }

    /**
     * 调用 Chat API
     */
    private String callChatApi(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", List.of(message));

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiBaseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").get(0).path("message").path("content");
            return content.asText("生成失败");
        } else {
            throw new RuntimeException("API调用失败，状态码: " + response.statusCode());
        }
    }
}
