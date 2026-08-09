package com.tom.wardrobe.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OrderSchemaInitializerTest {

    @Test
    void initCreatesOrderTableAndExpandsClothesDetails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OrderSchemaInitializer initializer = new OrderSchemaInitializer(jdbcTemplate);

        initializer.init();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).execute(sqlCaptor.capture());

        assertTrue(sqlCaptor.getAllValues().stream().anyMatch(sql ->
                sql.contains("CREATE TABLE IF NOT EXISTS t_order")
                        && sql.contains("clothes_details TEXT")));
        assertTrue(sqlCaptor.getAllValues().stream().anyMatch(sql ->
                sql.contains("ALTER TABLE t_order")
                        && sql.contains("MODIFY COLUMN clothes_details TEXT")));
    }
}
