package com.tom.wardrobe.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.tom.wardrobe.annotation.Idempotent;
import com.tom.wardrobe.entity.Order;
import com.tom.wardrobe.service.OrderService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 获取当前登录用户的订单列表
     * 修复水平越权：不再从前端接收 userId
     */
    @GetMapping
    public List<Order> getOrders() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return orderService.findByUserId(userId);
    }

    /**
     * 获取单个订单详情（用户只能查看自己的订单）
     */
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Integer id) {
        Order order = orderService.findById(id);
        if (order != null) {
            Integer userId = StpUtil.getLoginIdAsInt();
            // 管理员可以查看所有订单，普通用户只能查看自己的
            if (!StpUtil.hasRole("admin") && !userId.equals(order.getUserId())) {
                return null;
            }
        }
        return order;
    }

    /**
     * 获取所有订单（管理员专用）
     */
    @SaCheckRole("admin")
    @GetMapping("/all")
    public List<Order> getAllOrders() {
        return orderService.findAll();
    }

    /**
     * 按状态查询订单（管理员专用）
     */
    @SaCheckRole("admin")
    @GetMapping("/status/{status}")
    public List<Order> getOrdersByStatus(@PathVariable Integer status) {
        return orderService.findByStatus(status);
    }

    /**
     * 创建订单
     * 修复水平越权：强制使用当前登录用户 ID
     * 幂等性：需要在 Header 中携带 X-Idempotent-Token
     */
    @Idempotent(timeout = 120)
    @PostMapping
    public String createOrder(@RequestBody Order order) {
        order.setUserId(StpUtil.getLoginIdAsInt());
        return orderService.createOrder(order);
    }

    /**
     * 更新订单（管理员专用）
     */
    @SaCheckRole("admin")
    @PutMapping
    public String updateOrder(@RequestBody Order order) {
        return orderService.updateOrder(order);
    }

    /**
     * 删除订单（管理员专用）
     */
    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public String deleteOrder(@PathVariable Integer id) {
        return orderService.deleteOrder(id);
    }

    /**
     * 支付订单（用户操作自己的订单）
     */
    @PutMapping("/pay/{id}")
    public String payOrder(@PathVariable Integer id) {
        return orderService.payOrder(id);
    }

    /**
     * 确认收货（用户操作自己的订单）
     */
    @PutMapping("/receive/{id}")
    public String receiveOrder(@PathVariable Integer id) {
        return orderService.receiveOrder(id);
    }

    /**
     * 发货（管理员专用）
     */
    @SaCheckRole("admin")
    @PutMapping("/delivery/{id}")
    public String deliveryOrder(@PathVariable Integer id) {
        return orderService.deliveryOrder(id);
    }
}