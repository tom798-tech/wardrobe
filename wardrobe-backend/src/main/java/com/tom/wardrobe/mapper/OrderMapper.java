package com.tom.wardrobe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.wardrobe.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM t_order WHERE user_id = #{userId}")
    List<Order> findByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM t_order WHERE status = #{status}")
    List<Order> findByStatus(@Param("status") Integer status);

    @Select("SELECT * FROM t_order WHERE user_id = #{userId} AND status = #{status}")
    List<Order> findByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") Integer status);
}