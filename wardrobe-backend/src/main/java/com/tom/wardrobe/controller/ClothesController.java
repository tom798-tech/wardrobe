package com.tom.wardrobe.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.tom.wardrobe.entity.Clothes;
import com.tom.wardrobe.service.ClothesService;
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
@RequestMapping("/clothes")
public class ClothesController {

    @Resource
    private ClothesService clothesService;

    @SaIgnore
    @GetMapping
    public List<Clothes> getAllClothes() {
        return clothesService.findAll();
    }

    @SaIgnore
    @GetMapping("/{id}")
    public Clothes getClothesById(@PathVariable Integer id) {
        return clothesService.findById(id);
    }

    @SaIgnore
    @GetMapping("/type/{typeId}")
    public List<Clothes> getClothesByType(@PathVariable Integer typeId) {
        return clothesService.findByTypeId(typeId);
    }

    @SaIgnore
    @GetMapping("/brand/{brandId}")
    public List<Clothes> getClothesByBrand(@PathVariable Integer brandId) {
        return clothesService.findByBrandId(brandId);
    }

    @SaIgnore
    @GetMapping("/search")
    public List<Clothes> searchClothes(@RequestParam String keyword) {
        return clothesService.searchByName(keyword);
    }

    @SaIgnore
    @GetMapping("/style/{style}")
    public List<Clothes> getClothesByStyle(@PathVariable String style) {
        return clothesService.findByStyle(style);
    }

    @SaCheckRole("admin")
    @PostMapping
    public String addClothes(@RequestBody Clothes clothes) {
        return clothesService.addClothes(clothes);
    }

    @SaCheckRole("admin")
    @PutMapping
    public String updateClothes(@RequestBody Clothes clothes) {
        return clothesService.updateClothes(clothes);
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public String deleteClothes(@PathVariable Integer id) {
        return clothesService.deleteClothes(id);
    }
}
