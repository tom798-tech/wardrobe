package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.Cart;
import com.tom.wardrobe.entity.Clothes;
import com.tom.wardrobe.mapper.CartMapper;
import com.tom.wardrobe.mapper.ClothesMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class CartService {

    @Resource
    private CartMapper cartMapper;

    @Resource
    private ClothesMapper clothesMapper;

    public List<Cart> findByUserId(Integer userId) {
        List<Cart> carts = cartMapper.findByUserId(userId);
        carts.forEach(cart -> {
            Clothes clothes = clothesMapper.selectById(cart.getClothId());
            cart.setClothes(clothes);
        });
        return carts;
    }

    public String addToCart(Cart cart) {
        Cart existing = cartMapper.findByUserIdAndClothId(cart.getUserId(), cart.getClothId(), cart.getClothSize());
        if (existing != null) {
            existing.setAmount(existing.getAmount() + cart.getAmount());
            cartMapper.updateById(existing);
            return "购物车商品数量更新成功！";
        }
        cartMapper.insert(cart);
        return "添加到购物车成功！";
    }

    public String updateCart(Cart cart) {
        int count = cartMapper.updateById(cart);
        return count > 0 ? "更新成功！" : "更新失败！";
    }

    public String deleteCart(Integer id) {
        int count = cartMapper.deleteById(id);
        return count > 0 ? "删除成功！" : "删除失败！";
    }

    public String clearCart(Integer userId) {
        List<Cart> carts = cartMapper.findByUserId(userId);
        carts.forEach(cart -> cartMapper.deleteById(cart.getId()));
        return "清空购物车成功！";
    }
}