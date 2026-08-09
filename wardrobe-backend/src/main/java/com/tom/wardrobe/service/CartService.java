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
            int targetAmount = (existing.getAmount() == null ? 0 : existing.getAmount()) + (cart.getAmount() == null ? 0 : cart.getAmount());
            String error = validateTotalStock(cart.getUserId(), cart.getClothId(), existing.getId(), targetAmount);
            if (error != null) {
                return error;
            }
            existing.setAmount(targetAmount);
            cartMapper.updateById(existing);
            return "购物车商品数量更新成功！";
        }
        String error = validateTotalStock(cart.getUserId(), cart.getClothId(), null, cart.getAmount());
        if (error != null) {
            return error;
        }
        cartMapper.insert(cart);
        return "添加到购物车成功！";
    }

    public String updateCart(Cart cart) {
        Cart existing = cartMapper.selectById(cart.getId());
        if (existing == null || !existing.getUserId().equals(cart.getUserId())) {
            return "购物车商品不存在！";
        }
        String error = validateTotalStock(cart.getUserId(), existing.getClothId(), existing.getId(), cart.getAmount());
        if (error != null) {
            return error;
        }
        existing.setAmount(cart.getAmount());
        int count = cartMapper.updateById(existing);
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

    private String validateTotalStock(Integer userId, Integer clothId, Integer currentCartId, Integer targetAmount) {
        if (targetAmount == null || targetAmount <= 0) {
            return "商品数量不正确！";
        }
        Clothes clothes = clothesMapper.selectById(clothId);
        if (clothes == null) {
            return "商品不存在！";
        }
        int stock = clothes.getStock() == null ? 0 : clothes.getStock();
        int total = targetAmount;
        for (Cart item : cartMapper.findByUserId(userId)) {
            if (!clothId.equals(item.getClothId())) {
                continue;
            }
            if (currentCartId != null && currentCartId.equals(item.getId())) {
                continue;
            }
            total += item.getAmount() == null ? 0 : item.getAmount();
        }
        if (total > stock) {
            return "库存不足！该服装总库存为 " + stock + " 件";
        }
        return null;
    }
}
