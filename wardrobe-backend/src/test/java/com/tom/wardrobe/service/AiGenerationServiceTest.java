package com.tom.wardrobe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AiGenerationService 单元测试
 * 测试AI生成描述、评论摘要等核心功能
 */
@ExtendWith(MockitoExtension.class)
class AiGenerationServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiGenerationService aiGenerationService;

    @Test
    @DisplayName("测试生成商品描述（使用fallback）")
    void testGenerateDescription_Fallback() {
        // 执行测试（由于外部API不可用，会触发fallback逻辑）
        String result = aiGenerationService.generateDescription(
                "夏季纯棉T恤",
                "T恤",
                "简约",
                "优衣库"
        );

        // 验证结果
        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(result.contains("夏季纯棉T恤") || result.contains("爆款"));
    }

    @Test
    @DisplayName("测试生成商品描述（参数为null）")
    void testGenerateDescription_NullParams() {
        // 执行测试
        String result = aiGenerationService.generateDescription(
                "牛仔裤",
                null,
                null,
                null
        );

        // 验证结果
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    @DisplayName("测试生成商品描述（空字符串参数）")
    void testGenerateDescription_EmptyParams() {
        // 执行测试
        String result = aiGenerationService.generateDescription(
                "连衣裙",
                "",
                "",
                ""
        );

        // 验证结果
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    @DisplayName("测试生成商品描述（中文参数）")
    void testGenerateDescription_ChineseParams() {
        // 执行测试
        String result = aiGenerationService.generateDescription(
                "秋冬羊毛大衣",
                "大衣",
                "优雅",
                "MaxMara"
        );

        // 验证结果
        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(result.length() < 200); // 验证长度限制
    }

    @Test
    @DisplayName("测试生成评论摘要")
    void testGenerateCommentSummary() {
        // 执行测试
        Map<String, Object> result = aiGenerationService.generateCommentSummary(1);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("positivePoints"));
        assertTrue(result.containsKey("negativePoints"));
        assertTrue(result.containsKey("summary"));

        // 验证数据结构
        List<String> positivePoints = (List<String>) result.get("positivePoints");
        List<String> negativePoints = (List<String>) result.get("negativePoints");
        
        assertNotNull(positivePoints);
        assertNotNull(negativePoints);
        assertFalse(positivePoints.isEmpty());
        
        assertTrue(result.get("summary") instanceof String);
    }

    @Test
    @DisplayName("测试生成评论摘要（不同商品ID）")
    void testGenerateCommentSummary_DifferentClothId() {
        // 执行测试
        Map<String, Object> result1 = aiGenerationService.generateCommentSummary(1);
        Map<String, Object> result2 = aiGenerationService.generateCommentSummary(999);

        // 验证结果
        assertNotNull(result1);
        assertNotNull(result2);
        
        // 验证两个结果结构一致
        assertEquals(result1.keySet(), result2.keySet());
        
        List<String> positive1 = (List<String>) result1.get("positivePoints");
        List<String> positive2 = (List<String>) result2.get("positivePoints");
        
        assertEquals(positive1.size(), positive2.size());
    }

    @Test
    @DisplayName("测试生成评论摘要（positivePoints内容）")
    void testGenerateCommentSummary_PositivePoints() {
        // 执行测试
        Map<String, Object> result = aiGenerationService.generateCommentSummary(1);

        // 验证好评内容
        List<String> positivePoints = (List<String>) result.get("positivePoints");
        assertNotNull(positivePoints);
        assertTrue(positivePoints.contains("质量不错"));
        assertTrue(positivePoints.contains("款式新颖"));
        assertTrue(positivePoints.contains("性价比高"));
    }

    @Test
    @DisplayName("测试生成评论摘要（negativePoints内容）")
    void testGenerateCommentSummary_NegativePoints() {
        // 执行测试
        Map<String, Object> result = aiGenerationService.generateCommentSummary(1);

        // 验证差评内容
        List<String> negativePoints = (List<String>) result.get("negativePoints");
        assertNotNull(negativePoints);
        assertTrue(negativePoints.contains("暂无差评"));
    }

    @Test
    @DisplayName("测试生成评论摘要（summary内容）")
    void testGenerateCommentSummary_Summary() {
        // 执行测试
        Map<String, Object> result = aiGenerationService.generateCommentSummary(1);

        // 验证摘要内容
        String summary = (String) result.get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("整体评价良好") || summary.contains("用户反馈"));
    }
}
