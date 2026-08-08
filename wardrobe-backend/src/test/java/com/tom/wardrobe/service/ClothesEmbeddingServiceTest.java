package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.Clothes;
import com.tom.wardrobe.mapper.ClothesMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ClothesEmbeddingService 单元测试
 * 测试向量搜索、相似推荐、降级搜索等核心功能
 */
@ExtendWith(MockitoExtension.class)
class ClothesEmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ClothesMapper clothesMapper;

    @InjectMocks
    private ClothesEmbeddingService clothesEmbeddingService;

    @Test
    @DisplayName("测试向量搜索（成功）")
    void testSearchByText_Success() {
        String query = "夏天T恤";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("clothId", 1);
        metadata.put("clothName", "夏季纯棉T恤");
        metadata.put("price", 99);
        
        Document doc1 = new Document("doc1", "商品名称：夏季纯棉T恤。风格：简约。价格：99元。", metadata);
        Document doc2 = new Document("doc2", "商品名称：运动T恤。风格：休闲。价格：129元。", new HashMap<>());
        
        when(vectorStore.similaritySearch(query)).thenReturn(Arrays.asList(doc1, doc2));

        List<Map<String, Object>> results = clothesEmbeddingService.searchByText(query, 2);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("夏季纯棉T恤", results.get(0).get("clothName"));
        verify(vectorStore, times(1)).similaritySearch(query);
    }

    @Test
    @DisplayName("测试向量搜索（返回数量超过topK）")
    void testSearchByText_LimitTopK() {
        String query = "T恤";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("clothId", 1);
        
        Document doc1 = new Document("doc1", "T恤1", metadata);
        Document doc2 = new Document("doc2", "T恤2", new HashMap<>());
        Document doc3 = new Document("doc3", "T恤3", new HashMap<>());
        
        when(vectorStore.similaritySearch(query)).thenReturn(Arrays.asList(doc1, doc2, doc3));

        List<Map<String, Object>> results = clothesEmbeddingService.searchByText(query, 2);

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("测试向量搜索（向量服务不可用，降级搜索）")
    void testSearchByText_Fallback() {
        // 创建一个不注入vectorStore的新实例来模拟服务不可用
        ClothesEmbeddingService fallbackService = new ClothesEmbeddingService();
        
        // 使用反射注入clothesMapper
        try {
            java.lang.reflect.Field mapperField = ClothesEmbeddingService.class.getDeclaredField("clothesMapper");
            mapperField.setAccessible(true);
            mapperField.set(fallbackService, clothesMapper);
        } catch (Exception e) {
            fail("反射注入失败: " + e.getMessage());
        }

        String query = "T恤";
        Clothes clothes = new Clothes();
        clothes.setId(1);
        clothes.setClothName("夏季纯棉T恤");
        clothes.setPrice(99.0);
        
        when(clothesMapper.searchByName(query)).thenReturn(Collections.singletonList(clothes));

        List<Map<String, Object>> results = fallbackService.searchByText(query, 5);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("夏季纯棉T恤", results.get(0).get("clothName"));
        assertEquals(1, results.get(0).get("id"));
    }

    @Test
    @DisplayName("测试相似推荐（成功）")
    void testRecommendSimilar_Success() {
        Integer clothId = 1;
        
        Clothes targetClothes = new Clothes();
        targetClothes.setId(clothId);
        targetClothes.setClothName("夏季纯棉T恤");
        targetClothes.setStyle("简约");
        targetClothes.setPrice(99.0);
        
        when(clothesMapper.selectById(clothId)).thenReturn(targetClothes);

        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("clothId", clothId);
        metadata1.put("clothName", "夏季纯棉T恤");
        
        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("clothId", 2);
        metadata2.put("clothName", "简约T恤");
        
        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("clothId", 3);
        metadata3.put("clothName", "纯棉上衣");
        
        Document doc1 = new Document("doc1", "夏季纯棉T恤", metadata1);
        Document doc2 = new Document("doc2", "简约T恤", metadata2);
        Document doc3 = new Document("doc3", "纯棉上衣", metadata3);
        
        when(vectorStore.similaritySearch(anyString())).thenReturn(Arrays.asList(doc1, doc2, doc3));

        List<Map<String, Object>> results = clothesEmbeddingService.recommendSimilar(clothId, 2);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertNotEquals(clothId, results.get(0).get("clothId"));
        assertNotEquals(clothId, results.get(1).get("clothId"));
    }

    @Test
    @DisplayName("测试相似推荐（商品不存在）")
    void testRecommendSimilar_ClothesNotFound() {
        Integer clothId = 999;
        when(clothesMapper.selectById(clothId)).thenReturn(null);

        List<Map<String, Object>> results = clothesEmbeddingService.recommendSimilar(clothId, 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("测试刷新向量库（成功）")
    void testRefreshEmbeddings_Success() {
        when(clothesMapper.selectList(null)).thenReturn(Collections.emptyList());

        String result = clothesEmbeddingService.refreshEmbeddings();

        assertEquals("向量库刷新完成", result);
    }

    @Test
    @DisplayName("测试刷新向量库（VectorStore未配置）")
    void testRefreshEmbeddings_VectorStoreNotConfigured() {
        ClothesEmbeddingService service = new ClothesEmbeddingService();
        
        try {
            java.lang.reflect.Field mapperField = ClothesEmbeddingService.class.getDeclaredField("clothesMapper");
            mapperField.setAccessible(true);
            mapperField.set(service, clothesMapper);
        } catch (Exception e) {
            fail("反射注入失败: " + e.getMessage());
        }

        String result = service.refreshEmbeddings();

        assertEquals("VectorStore 未配置", result);
    }

    @Test
    @DisplayName("测试获取向量服务状态")
    void testGetStatus() {
        Map<String, Object> status = clothesEmbeddingService.getStatus();

        assertNotNull(status);
        assertTrue((Boolean) status.get("embeddingModelAvailable"));
        assertTrue((Boolean) status.get("vectorStoreAvailable"));
    }

    @Test
    @DisplayName("测试获取向量服务状态（服务未配置）")
    void testGetStatus_NotAvailable() {
        ClothesEmbeddingService service = new ClothesEmbeddingService();
        
        Map<String, Object> status = service.getStatus();

        assertNotNull(status);
        assertFalse((Boolean) status.get("embeddingModelAvailable"));
        assertFalse((Boolean) status.get("vectorStoreAvailable"));
    }

    @Test
    @DisplayName("测试构建内容方法（通过降级搜索间接测试）")
    void testBuildContent() {
        ClothesEmbeddingService fallbackService = new ClothesEmbeddingService();
        
        try {
            java.lang.reflect.Field mapperField = ClothesEmbeddingService.class.getDeclaredField("clothesMapper");
            mapperField.setAccessible(true);
            mapperField.set(fallbackService, clothesMapper);
        } catch (Exception e) {
            fail("反射注入失败: " + e.getMessage());
        }

        Clothes clothes = new Clothes();
        clothes.setId(1);
        clothes.setClothName("纯棉T恤");
        clothes.setStyle("简约");
        clothes.setPrice(99.0);

        String query = "纯棉T恤";
        when(clothesMapper.searchByName(query)).thenReturn(Collections.singletonList(clothes));

        List<Map<String, Object>> results = fallbackService.searchByText(query, 1);
        
        assertNotNull(results);
        assertEquals("纯棉T恤", results.get(0).get("clothName"));
    }
}
