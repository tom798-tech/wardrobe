package com.tom.wardrobe.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderOutboxSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public OrderOutboxSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS t_order_outbox (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  aggregate_type VARCHAR(64) NOT NULL,
                  aggregate_id BIGINT NOT NULL,
                  event_type VARCHAR(64) NOT NULL,
                  payload VARCHAR(255) NOT NULL,
                  status TINYINT NOT NULL DEFAULT 0,
                  retry_count INT NOT NULL DEFAULT 0,
                  next_retry_time DATETIME NOT NULL,
                  last_error VARCHAR(512) NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  KEY idx_order_outbox_status_next_retry (status, next_retry_time),
                  KEY idx_order_outbox_aggregate (aggregate_type, aggregate_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
