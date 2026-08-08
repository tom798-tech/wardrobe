package com.tom.wardrobe.controller;

import com.tom.wardrobe.service.AiGenerationService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 功能控制器
 * 提供商品描述生成、评论摘要等 AI 接口
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiGenerationService aiGenerationService;

    /**
     * 生成商品描述
     *
     * @param request 请求参数
     * @return 生成的商品描述
     */
    @PostMapping("/generate-description")
    public Map<String, Object> generateDescription(@RequestBody GenerateDescriptionRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String description = aiGenerationService.generateDescription(
                    request.getClothName(),
                    request.getTypeName(),
                    request.getStyle(),
                    request.getBrand()
            );
            result.put("success", true);
            result.put("description", description);
        } catch (Exception e) {
            log.error("生成商品描述失败", e);
            result.put("success", false);
            result.put("message", "生成失败");
        }
        return result;
    }

    /**
     * 获取商品评论摘要
     *
     * @param clothId 商品ID
     * @return 评论摘要
     */
    @GetMapping("/comment-summary/{clothId}")
    public Map<String, Object> getCommentSummary(@PathVariable Integer clothId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> summary = aiGenerationService.generateCommentSummary(clothId);
            result.put("success", true);
            result.put("data", summary);
        } catch (Exception e) {
            log.error("获取评论摘要失败，clothId: {}", clothId, e);
            result.put("success", false);
            result.put("message", "获取失败");
        }
        return result;
    }

    /**
     * 生成描述请求参数
     */
    @Data
    public static class GenerateDescriptionRequest {
        private String clothName;
        private String typeName;
        private String style;
        private String brand;
    }
}