package com.tom.wardrobe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 排除 Spring AI pgvector 的自动数据源配置，避免覆盖 MySQL 主数据源
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration.class
})
@EnableScheduling
public class WardrobeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WardrobeApplication.class, args);
    }
}
