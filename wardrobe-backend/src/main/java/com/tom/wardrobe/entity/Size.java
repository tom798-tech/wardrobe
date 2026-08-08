package com.tom.wardrobe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_size")
public class Size {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("size_name")
    private String sizeName;

    @TableField("type_id")
    private Integer typeId;

    /** 兼容前端 sizeValue 字段名：值等同于 sizeName */
    @TableField(exist = false)
    private String sizeValue;

    /** 兼容前端 intro 字段（可选描述） */
    @TableField(exist = false)
    private String intro;

    // --- 覆盖 Lombok 默认 getter/setter：sizeValue <-> sizeName 同步 ---
    public String getSizeValue() {
        return this.sizeName;
    }

    public void setSizeValue(String sizeValue) {
        this.sizeName = sizeValue;
        this.sizeValue = sizeValue;
    }
}