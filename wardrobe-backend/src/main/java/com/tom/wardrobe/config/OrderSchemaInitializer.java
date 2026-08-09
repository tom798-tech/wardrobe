package com.tom.wardrobe.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public OrderSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS t_order (
                  id INT NOT NULL AUTO_INCREMENT COMMENT 'order id',
                  clothes_details TEXT NULL COMMENT 'clothes details',
                  price DECIMAL(10, 2) NULL DEFAULT NULL COMMENT 'order price',
                  status VARCHAR(255) NULL DEFAULT NULL COMMENT 'order status',
                  user_id INT NULL DEFAULT NULL COMMENT 'user id',
                  address VARCHAR(255) NULL DEFAULT NULL COMMENT 'shipping address',
                  time VARCHAR(255) NULL DEFAULT NULL COMMENT 'submit time',
                  PRIMARY KEY (id),
                  KEY idx_order_user (user_id),
                  KEY idx_order_status (status),
                  KEY idx_order_user_status (user_id, status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                ALTER TABLE t_order
                MODIFY COLUMN clothes_details TEXT NULL COMMENT 'clothes details'
                """);
    }
}
