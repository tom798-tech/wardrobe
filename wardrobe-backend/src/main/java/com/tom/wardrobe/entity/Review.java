package com.tom.wardrobe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("cloth_id")
    private Integer clothId;

    private String content;

    private Integer rating;

    @TableField("create_time")
    private String createTime;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String clothName;
}