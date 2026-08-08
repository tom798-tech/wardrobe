package com.tom.wardrobe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("cloth_id")
    private Integer clothId;

    @TableField("cloth_size")
    private String clothSize;

    private Integer amount;

    @TableField("user_id")
    private Integer userId;

    private String date;

    @TableField(exist = false)
    private Clothes clothes;
}