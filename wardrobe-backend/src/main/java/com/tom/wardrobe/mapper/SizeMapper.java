package com.tom.wardrobe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.wardrobe.entity.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SizeMapper extends BaseMapper<Size> {

    @Select("SELECT * FROM t_size WHERE type_id = #{typeId}")
    List<Size> findByTypeId(@Param("typeId") Integer typeId);
}