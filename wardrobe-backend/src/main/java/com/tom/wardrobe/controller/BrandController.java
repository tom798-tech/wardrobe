package com.tom.wardrobe.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.tom.wardrobe.entity.Brand;
import com.tom.wardrobe.service.BrandService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/brand")
public class BrandController {

    @Resource
    private BrandService brandService;

    @SaIgnore
    @GetMapping
    public List<Brand> getAllBrands() {
        return brandService.findAll();
    }

    @SaIgnore
    @GetMapping("/{id}")
    public Brand getBrandById(@PathVariable Integer id) {
        return brandService.findById(id);
    }

    @SaIgnore
    @GetMapping("/search")
    public List<Brand> searchBrand(@RequestParam String keyword) {
        return brandService.searchByName(keyword);
    }

    @SaCheckRole("admin")
    @PostMapping
    public String addBrand(@RequestBody Brand brand) {
        return brandService.addBrand(brand);
    }

    @SaCheckRole("admin")
    @PutMapping
    public String updateBrand(@RequestBody Brand brand) {
        return brandService.updateBrand(brand);
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public String deleteBrand(@PathVariable Integer id) {
        return brandService.deleteBrand(id);
    }
}
