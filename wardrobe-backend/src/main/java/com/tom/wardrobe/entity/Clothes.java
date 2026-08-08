package com.tom.wardrobe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("t_clothes")
public class Clothes {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("cloth_name")
    private String clothName;

    private String image;

    @TableField(exist = false)
    private String images;

    @TableField("type_id")
    private Integer typeId;

    @TableField("brand_id")
    private Integer brandId;

    private String style;

    private Double price;

    private String description;

    private Integer stock;

    private Integer sales;

    @TableField(exist = false)
    private List<Size> sizeList;
}
