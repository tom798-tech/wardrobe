package com.tom.wardrobe.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.tom.wardrobe.service.ClothesEmbeddingService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 商品向量检索控制器
 * 提供以文搜衣、相似推荐等 AI 功能接口
 */
@RestController
@RequestMapping("/vector")
@SaIgnore
public class ClothesVectorController {

    @Resource
    private ClothesEmbeddingService clothesEmbeddingService;

    /**
     * 以文搜衣 - 根据文本描述搜索相似商品
     * GET /vector/search?query=夏天纯棉蓝色T恤&topK=5
     *
     * @param query 搜索文本
     * @param topK  返回数量，默认 5
     */
    @GetMapping("/search")
    public List<Map<String, Object>> searchByText(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        return clothesEmbeddingService.searchByText(query, topK);
    }

    /**
     * 相似推荐 - 根据商品 ID 推荐相似商品
     * GET /vector/recommend/{clothId}?topK=5
     *
     * @param clothId 商品 ID
     * @param topK    返回数量，默认 5
     */
    @GetMapping("/recommend/{clothId}")
    public List<Map<String, Object>> recommendSimilar(
            @PathVariable Integer clothId,
            @RequestParam(defaultValue = "5") int topK) {
        return clothesEmbeddingService.recommendSimilar(clothId, topK);
    }

    /**
     * 刷新向量库（重新导入所有商品）
     * POST /vector/refresh
     */
    @PostMapping("/refresh")
    public String refreshEmbeddings() {
        return clothesEmbeddingService.refreshEmbeddings();
    }

    /**
     * 检查向量服务状态
     * GET /vector/status
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return clothesEmbeddingService.getStatus();
    }
}