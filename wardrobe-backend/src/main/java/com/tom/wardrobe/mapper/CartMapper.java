package com.tom.wardrobe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.wardrobe.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    @Select("SELECT * FROM t_cart WHERE user_id = #{userId}")
    List<Cart> findByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM t_cart WHERE user_id = #{userId} AND cloth_id = #{clothId} AND cloth_size = #{clothSize}")
    Cart findByUserIdAndClothId(@Param("userId") Integer userId, @Param("clothId") Integer clothId, @Param("clothSize") String clothSize);
}