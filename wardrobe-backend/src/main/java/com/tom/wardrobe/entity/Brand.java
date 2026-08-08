package com.tom.wardrobe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_brand")
public class Brand {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("brand_name")
    private String brandName;

    @TableField("brand_logo")
    private String brandLogo;

    private String description;

    @TableField("create_time")
    private String createTime;
}