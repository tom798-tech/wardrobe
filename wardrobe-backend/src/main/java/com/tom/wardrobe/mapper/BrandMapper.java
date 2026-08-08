package com.tom.wardrobe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.wardrobe.entity.Brand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrandMapper extends BaseMapper<Brand> {

    @Select("SELECT * FROM t_brand WHERE brand_name LIKE CONCAT('%', #{keyword}, '%')")
    List<Brand> searchByName(@Param("keyword") String keyword);
}