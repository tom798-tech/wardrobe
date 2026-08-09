package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.Clothes;
import com.tom.wardrobe.entity.Size;
import com.tom.wardrobe.mapper.ClothesMapper;
import com.tom.wardrobe.mapper.SizeMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
@CacheConfig(cacheNames = "clothes")
public class ClothesService {

    @Resource
    private ClothesMapper clothesMapper;

    @Resource
    private SizeMapper sizeMapper;

    @Resource
    private StockService stockService;

    @Cacheable(key = "'all'")
    public List<Clothes> findAll() {
        return clothesMapper.selectList(null);
    }

    @Cacheable(key = "#id")
    public Clothes findById(Integer id) {
        Clothes clothes = clothesMapper.selectById(id);
        if (clothes != null && clothes.getTypeId() != null) {
            List<Size> sizeList = sizeMapper.findByTypeId(clothes.getTypeId());
            clothes.setSizeList(sizeList);
        }
        return clothes;
    }

    @Cacheable(key = "'type:' + #typeId")
    public List<Clothes> findByTypeId(Integer typeId) {
        return clothesMapper.findByTypeId(typeId);
    }

    @Cacheable(key = "'brand:' + #brandId")
    public List<Clothes> findByBrandId(Integer brandId) {
        return clothesMapper.findByBrandId(brandId);
    }

    public List<Clothes> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String normalizedKeyword = keyword.trim();
        try {
            return clothesMapper.searchByName(normalizedKeyword);
        } catch (DataAccessException ex) {
            return clothesMapper.searchByNameLike(normalizedKeyword);
        }
    }

    @Cacheable(key = "'style:' + #style")
    public List<Clothes> findByStyle(String style) {
        return clothesMapper.findByStyle(style);
    }

    @CacheEvict(allEntries = true)
    public String addClothes(Clothes clothes) {
        if (clothes.getStock() == null) {
            clothes.setStock(0);
        }
        int count = clothesMapper.insert(clothes);
        if (count > 0) {
            stockService.initStock(clothes.getId(), clothes.getStock());
        }
        return count > 0 ? "添加成功！" : "添加失败！";
    }

    @CacheEvict(allEntries = true)
    public String updateClothes(Clothes clothes) {
        int count = clothesMapper.updateById(clothes);
        if (count > 0 && clothes.getStock() != null) {
            stockService.syncStockFromDb(clothes.getId());
        }
        return count > 0 ? "修改成功！" : "修改失败！";
    }

    @CacheEvict(allEntries = true)
    public String deleteClothes(Integer id) {
        int count = clothesMapper.deleteById(id);
        return count > 0 ? "删除成功！" : "删除失败！";
    }
}
