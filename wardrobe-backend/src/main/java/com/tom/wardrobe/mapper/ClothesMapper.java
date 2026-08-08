package com.tom.wardrobe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.wardrobe.entity.Clothes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClothesMapper extends BaseMapper<Clothes> {

    @Select("SELECT * FROM t_clothes WHERE type_id = #{typeId}")
    List<Clothes> findByTypeId(@Param("typeId") Integer typeId);

    @Select("SELECT * FROM t_clothes WHERE brand_id = #{brandId}")
    List<Clothes> findByBrandId(@Param("brandId") Integer brandId);

    @Select("""
            SELECT *
            FROM t_clothes
            WHERE MATCH(cloth_name, style, description)
                      AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)
               OR cloth_name LIKE CONCAT('%', #{keyword}, '%')
               OR style LIKE CONCAT('%', #{keyword}, '%')
               OR COALESCE(description, '') LIKE CONCAT('%', #{keyword}, '%')
            ORDER BY
                MATCH(cloth_name, style, description)
                    AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) DESC,
                sales DESC,
                id DESC
            LIMIT 50
            """)
    List<Clothes> searchByName(@Param("keyword") String keyword);

    @Select("""
            SELECT *
            FROM t_clothes
            WHERE cloth_name LIKE CONCAT('%', #{keyword}, '%')
               OR style LIKE CONCAT('%', #{keyword}, '%')
               OR COALESCE(description, '') LIKE CONCAT('%', #{keyword}, '%')
            ORDER BY sales DESC, id DESC
            LIMIT 50
            """)
    List<Clothes> searchByNameLike(@Param("keyword") String keyword);

    @Select("SELECT * FROM t_clothes WHERE style = #{style}")
    List<Clothes> findByStyle(@Param("style") String style);
}
