package com.tom.wardrobe.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.tom.wardrobe.entity.Size;
import com.tom.wardrobe.service.SizeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/size")
public class SizeController {

    @Resource
    private SizeService sizeService;

    @SaIgnore
    @GetMapping
    public List<Size> getAllSizes() {
        return sizeService.findAll();
    }

    @SaIgnore
    @GetMapping("/type/{typeId}")
    public List<Size> getSizesByTypeId(@PathVariable Integer typeId) {
        return sizeService.findByTypeId(typeId);
    }

    @SaIgnore
    @GetMapping("/{id}")
    public Size getSizeById(@PathVariable Integer id) {
        return sizeService.findById(id);
    }

    @SaCheckRole("admin")
    @PostMapping
    public String addSize(@RequestBody Size size) {
        return sizeService.addSize(size);
    }

    @SaCheckRole("admin")
    @PutMapping
    public String updateSize(@RequestBody Size size) {
        return sizeService.updateSize(size);
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public String deleteSize(@PathVariable Integer id) {
        return sizeService.deleteSize(id);
    }
}
