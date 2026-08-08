package com.tom.wardrobe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.wardrobe.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT * FROM t_review WHERE cloth_id = #{clothId}")
    List<Review> findByClothId(@Param("clothId") Integer clothId);

    @Select("SELECT r.*, u.user_name AS userName, c.cloth_name AS clothName FROM t_review r LEFT JOIN t_user u ON r.user_id = u.id LEFT JOIN t_clothes c ON r.cloth_id = c.id WHERE r.cloth_id = #{clothId}")
    List<Review> findByClothIdWithUserAndCloth(@Param("clothId") Integer clothId);

    @Select("SELECT r.*, u.user_name AS userName, c.cloth_name AS clothName FROM t_review r LEFT JOIN t_user u ON r.user_id = u.id LEFT JOIN t_clothes c ON r.cloth_id = c.id")
    List<Review> findAllWithUserAndCloth();
}