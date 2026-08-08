package com.tom.wardrobe.entity;

import lombok.Data;

/**
 * 订单商品项
 * 用于解析订单详情 JSON
 */
@Data
public class OrderItem {

    /**
     * 商品ID
     */
    private Integer clothId;

    /**
     * 商品名称
     */
    private String clothName;

    /**
     * 购买数量
     */
    private Integer amount;

    /**
     * 商品价格
     */
    private Double price;

    /**
     * 商品尺码
     */
    private String clothSize;
}
