package com.tom.wardrobe.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClothesMapperTest {

    @Test
    void searchQueryShouldUseFullTextWithKeywordFallback() throws Exception {
        Select select = ClothesMapper.class
                .getDeclaredMethod("searchByName", String.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", Arrays.stream(select.value()).toList()).toUpperCase();

        assertTrue(sql.contains("MATCH"));
        assertTrue(sql.contains("AGAINST"));
        assertTrue(sql.contains("LIKE"));
        assertTrue(sql.contains("DESCRIPTION"));
    }
}
