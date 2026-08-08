package com.tom.wardrobe.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ClothesEntityTest {

    @Test
    void businessFieldsShouldMapToRealTableColumns() throws Exception {
        Field brandIdField = Clothes.class.getDeclaredField("brandId");
        TableField brandIdTableField = brandIdField.getAnnotation(TableField.class);
        assertNotNull(brandIdTableField);
        assertEquals("brand_id", brandIdTableField.value());
        assertTrue(brandIdTableField.exist());

        assertNull(Clothes.class.getDeclaredField("description").getAnnotation(TableField.class));
        assertNull(Clothes.class.getDeclaredField("stock").getAnnotation(TableField.class));
        assertNull(Clothes.class.getDeclaredField("sales").getAnnotation(TableField.class));
    }
}
