package com.tom.wardrobe.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 向量数据库配置
 * 使用独立的 PostgreSQL 数据源
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(@Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate, EmbeddingModel embeddingModel) {
        // 手动创建向量表结构
        createVectorTable(pgJdbcTemplate);
        // 使用构造器创建 PgVectorStore
        return new PgVectorStore(pgJdbcTemplate, embeddingModel);
    }

    private void createVectorTable(JdbcTemplate jdbcTemplate) {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS public.vector_store (
                id UUID PRIMARY KEY,
                content TEXT,
                metadata JSONB,
                embedding vector(1536)
            );
            """;
        try {
            jdbcTemplate.execute(createTableSql);
        } catch (Exception e) {
            // 表已存在或创建失败，忽略
        }
    }
}
