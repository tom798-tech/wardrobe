package com.tom.wardrobe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("clothes_details")
    private String clothesDetails;

    private Double price;

    private Integer status;

    @TableField("user_id")
    private Integer userId;

    private String address;

    private String time;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String phone;
}