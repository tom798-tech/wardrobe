package com.tom.wardrobe.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tom.wardrobe.entity.Cart;
import com.tom.wardrobe.service.CartService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Resource
    private CartService cartService;

    /**
     * 获取当前登录用户的购物车
     * 修复水平越权：不再从前端接收 userId，而是从登录态获取
     */
    @GetMapping
    public List<Cart> getCart() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return cartService.findByUserId(userId);
    }

    /**
     * 添加商品到购物车
     * 修复水平越权：强制使用当前登录用户 ID
     */
    @PostMapping
    public String addToCart(@RequestBody Cart cart) {
        cart.setUserId(StpUtil.getLoginIdAsInt());
        return cartService.addToCart(cart);
    }

    /**
     * 更新购物车
     * 修复水平越权：强制使用当前登录用户 ID
     */
    @PutMapping
    public String updateCart(@RequestBody Cart cart) {
        cart.setUserId(StpUtil.getLoginIdAsInt());
        return cartService.updateCart(cart);
    }

    /**
     * 删除购物车项
     */
    @DeleteMapping("/{id}")
    public String deleteCart(@PathVariable Integer id) {
        return cartService.deleteCart(id);
    }

    /**
     * 清空当前登录用户的购物车
     * 修复水平越权：不再从前端接收 userId
     */
    @DeleteMapping("/clear")
    public String clearCart() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return cartService.clearCart(userId);
    }
}