package com.tom.wardrobe.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClothesSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ClothesSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public ClothesSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS t_clothes (
                  id INT NOT NULL AUTO_INCREMENT COMMENT '服装ID',
                  cloth_name VARCHAR(255) NULL DEFAULT NULL COMMENT '服装名称',
                  image VARCHAR(255) NULL DEFAULT NULL COMMENT '服装图片',
                  type_id INT NULL DEFAULT NULL COMMENT '服装类别',
                  brand_id INT NULL DEFAULT NULL COMMENT '品牌ID',
                  style VARCHAR(255) NULL DEFAULT NULL COMMENT '服装风格',
                  price DECIMAL(10, 2) NULL DEFAULT NULL COMMENT '服装价格',
                  description VARCHAR(1000) NULL DEFAULT NULL COMMENT '商品描述',
                  stock INT NOT NULL DEFAULT 100 COMMENT '库存',
                  sales INT NOT NULL DEFAULT 0 COMMENT '销量',
                  PRIMARY KEY (id),
                  KEY idx_clothes_type (type_id),
                  KEY idx_clothes_style (style),
                  KEY idx_clothes_brand (brand_id),
                  KEY idx_clothes_brand_type (brand_id, type_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        addColumnIfMissing("brand_id",
                "ALTER TABLE t_clothes ADD COLUMN brand_id INT NULL DEFAULT NULL COMMENT '品牌ID' AFTER type_id");
        addColumnIfMissing("description",
                "ALTER TABLE t_clothes ADD COLUMN description VARCHAR(1000) NULL DEFAULT NULL COMMENT '商品描述' AFTER price");
        addColumnIfMissing("stock",
                "ALTER TABLE t_clothes ADD COLUMN stock INT NOT NULL DEFAULT 100 COMMENT '库存' AFTER description");
        addColumnIfMissing("sales",
                "ALTER TABLE t_clothes ADD COLUMN sales INT NOT NULL DEFAULT 0 COMMENT '销量' AFTER stock");

        addIndexIfMissing("idx_clothes_brand",
                "CREATE INDEX idx_clothes_brand ON t_clothes (brand_id)");
        addIndexIfMissing("idx_clothes_brand_type",
                "CREATE INDEX idx_clothes_brand_type ON t_clothes (brand_id, type_id)");
        try {
            addIndexIfMissing("ft_clothes_search",
                    "CREATE FULLTEXT INDEX ft_clothes_search ON t_clothes (cloth_name, style, description)");
        } catch (DataAccessException ex) {
            log.warn("创建商品全文索引失败，将保留关键词降级搜索", ex);
        }

        jdbcTemplate.execute("""
                UPDATE t_clothes
                SET brand_id = CASE
                        WHEN style = '运动' THEN 1
                        WHEN style = '正式' THEN 3
                        ELSE 2
                    END,
                    description = CASE
                        WHEN description IS NULL OR description = ''
                            THEN CONCAT(COALESCE(cloth_name, ''), '，', COALESCE(style, ''), '风格商品，适合日常穿搭与场景搭配。')
                        ELSE description
                    END,
                    stock = CASE
                        WHEN stock IS NULL THEN
                            CASE
                                WHEN type_id = 1 THEN 120
                                WHEN type_id = 2 THEN 80
                                WHEN type_id = 3 THEN 60
                                ELSE 100
                            END
                        ELSE stock
                    END,
                    sales = CASE
                        WHEN sales IS NULL THEN MOD(id * 7, 120)
                        ELSE sales
                    END
                WHERE brand_id IS NULL
                   OR description IS NULL
                   OR description = ''
                   OR stock IS NULL
                   OR sales IS NULL
                """);
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 't_clothes'
                  AND column_name = ?
                """, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private void addIndexIfMissing(String indexName, String createSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 't_clothes'
                  AND index_name = ?
                """, Integer.class, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(createSql);
        }
    }
}
