package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.Clothes;
import com.tom.wardrobe.mapper.ClothesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品向量服务 - 基于 Spring AI + pgvector 实现
 * 功能：以文搜衣、相似推荐、向量初始化
 */
@Service
public class ClothesEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ClothesEmbeddingService.class);

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired
    private ClothesMapper clothesMapper;

    /**
     * 将商品数据初始化到向量库
     * 使用 @PostConstruct 在 Bean 初始化后执行（可改为手动触发）
     */
    @PostConstruct
    public void initEmbeddings() {
        if (vectorStore == null) {
            log.warn("VectorStore 未配置，跳过向量初始化");
            return;
        }

        try {
            List<Clothes> clothesList = clothesMapper.selectList(null);
            if (clothesList.isEmpty()) {
                log.info("无商品数据，跳过向量初始化");
                return;
            }

            log.info("开始初始化向量库，共 {} 件商品", clothesList.size());

            List<Document> documents = clothesList.stream()
                    .map(this::clothesToDocument)
                    .collect(Collectors.toList());

            // 获取所有商品ID用于删除旧数据（幂等设计），使用相同的UUID格式
            List<String> ids = clothesList.stream()
                    .map(c -> UUID.nameUUIDFromBytes(("clothes_" + c.getId()).getBytes()).toString())
                    .collect(Collectors.toList());

            // 先尝试删除旧数据，如果表不存在则跳过
            try {
                vectorStore.delete(ids);
            } catch (Exception e) {
                log.warn("删除旧向量数据失败，可能是首次初始化", e);
            }

            // 添加新数据（会自动创建表）
            vectorStore.add(documents);

            log.info("向量库初始化完成，共写入 {} 条文档", documents.size());
        } catch (Exception e) {
            log.error("向量库初始化失败", e);
        }
    }

    /**
     * 商品转 Document（content + metadata）
     */
    private Document clothesToDocument(Clothes clothes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("clothId", clothes.getId());
        metadata.put("clothName", clothes.getClothName());
        metadata.put("typeId", clothes.getTypeId());
        if (clothes.getBrandId() != null) {
            metadata.put("brandId", clothes.getBrandId());
        }
        if (clothes.getPrice() != null) {
            metadata.put("price", clothes.getPrice());
        }
        if (clothes.getStyle() != null) {
            metadata.put("style", clothes.getStyle());
        }
        if (clothes.getImage() != null) {
            metadata.put("image", clothes.getImage());
        }
        metadata.put("entityType", "clothes");

        // 拼接文本内容用于向量化
        String content = buildContent(clothes);

        // 使用 UUID 格式作为文档 ID（PgVectorStore 要求 UUID）
        // 使用 clothesId 生成固定的 UUID，确保每次刷新都能正确匹配删除
        String documentId = UUID.nameUUIDFromBytes(("clothes_" + clothes.getId()).getBytes()).toString();

        // 使用 Document(id, text, metadata) 构造器设置文档 ID
        return new Document(documentId, content, metadata);
    }

    /**
     * 构建用于向量化的文本内容
     */
    private String buildContent(Clothes clothes) {
        StringBuilder sb = new StringBuilder();
        sb.append("商品名称：").append(clothes.getClothName()).append("。");
        if (clothes.getStyle() != null) {
            sb.append("风格：").append(clothes.getStyle()).append("。");
        }
        if (clothes.getDescription() != null) {
            sb.append("描述：").append(clothes.getDescription()).append("。");
        }
        sb.append("价格：").append(clothes.getPrice()).append("元。");
        return sb.toString();
    }

    /**
     * 以文搜衣 - 根据文本描述搜索相似商品
     */
    public List<Map<String, Object>> searchByText(String query, int topK) {
        if (vectorStore == null) {
            log.warn("VectorStore 未配置，降级为关键词搜索");
            return fallbackSearch(query, topK);
        }

        try {
            // 使用 similaritySearch(String) 方法
            List<Document> results = vectorStore.similaritySearch(query);

            // 手动截断到指定数量
            if (results.size() > topK) {
                results = results.subList(0, topK);
            }

            return results.stream()
                    .map(doc -> doc.getMetadata())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("向量搜索失败，降级为关键词搜索", e);
            return fallbackSearch(query, topK);
        }
    }

    /**
     * 相似推荐 - 根据商品 ID 推荐相似商品
     */
    public List<Map<String, Object>> recommendSimilar(Integer clothId, int topK) {
        Clothes clothes = clothesMapper.selectById(clothId);
        if (clothes == null) {
            return Collections.emptyList();
        }

        // 使用商品信息作为查询文本
        String query = buildContent(clothes);
        List<Map<String, Object>> results = searchByText(query, topK + 1);

        // 过滤掉自身（兼容向量搜索的 clothId 和 fallback 的 id）
        return results.stream()
                .filter(m -> !clothId.equals(m.get("clothId")) && !clothId.equals(m.get("id")))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 降级搜索 - 当向量服务不可用时使用关键词搜索
     */
    private List<Map<String, Object>> fallbackSearch(String query, int topK) {
        List<Clothes> clothesList = clothesMapper.searchByName(query);
        if (clothesList.size() > topK) {
            clothesList = clothesList.subList(0, topK);
        }

        return clothesList.stream()
                .map(this::clothesToMap)
                .collect(Collectors.toList());
    }

    /**
     * 商品转 Map（用于返回给前端）
     */
    private Map<String, Object> clothesToMap(Clothes clothes) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", clothes.getId());
        map.put("clothName", clothes.getClothName());
        map.put("typeId", clothes.getTypeId());
        map.put("brandId", clothes.getBrandId());
        map.put("price", clothes.getPrice());
        map.put("style", clothes.getStyle());
        map.put("stock", clothes.getStock());
        map.put("sales", clothes.getSales());
        map.put("image", clothes.getImage());
        map.put("description", clothes.getDescription());
        return map;
    }

    /**
     * 刷新向量库（重新导入所有商品）
     */
    public String refreshEmbeddings() {
        if (vectorStore == null) {
            return "VectorStore 未配置";
        }

        try {
            initEmbeddings();
            return "向量库刷新完成";
        } catch (Exception e) {
            log.error("向量库刷新失败", e);
            return "向量库刷新失败：" + e.getMessage();
        }
    }

    /**
     * 检查向量服务状态
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("embeddingModelAvailable", embeddingModel != null);
        status.put("vectorStoreAvailable", vectorStore != null);
        return status;
    }
}