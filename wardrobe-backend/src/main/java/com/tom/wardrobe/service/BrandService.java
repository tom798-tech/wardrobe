package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.Brand;
import com.tom.wardrobe.mapper.BrandMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BrandService {

    @Resource
    private BrandMapper brandMapper;

    public List<Brand> findAll() {
        return brandMapper.selectList(null);
    }

    public Brand findById(Integer id) {
        return brandMapper.selectById(id);
    }

    public List<Brand> searchByName(String keyword) {
        return brandMapper.searchByName(keyword);
    }

    public String addBrand(Brand brand) {
        brand.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        int count = brandMapper.insert(brand);
        return count > 0 ? "添加成功！" : "添加失败！";
    }

    public String updateBrand(Brand brand) {
        int count = brandMapper.updateById(brand);
        return count > 0 ? "修改成功！" : "修改失败！";
    }

    public String deleteBrand(Integer id) {
        int count = brandMapper.deleteById(id);
        return count > 0 ? "删除成功！" : "删除失败！";
    }
}