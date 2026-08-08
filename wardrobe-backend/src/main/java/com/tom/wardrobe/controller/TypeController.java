package com.tom.wardrobe.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.tom.wardrobe.entity.Type;
import com.tom.wardrobe.service.TypeService;
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
@RequestMapping("/type")
public class TypeController {

    @Resource
    private TypeService typeService;

    @SaIgnore
    @GetMapping
    public List<Type> getAllTypes() {
        return typeService.findAll();
    }

    @SaIgnore
    @GetMapping("/{id}")
    public Type getTypeById(@PathVariable Integer id) {
        return typeService.findById(id);
    }

    @SaCheckRole("admin")
    @PostMapping
    public String addType(@RequestBody Type type) {
        return typeService.addType(type);
    }

    @SaCheckRole("admin")
    @PutMapping
    public String updateType(@RequestBody Type type) {
        return typeService.updateType(type);
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public String deleteType(@PathVariable Integer id) {
        return typeService.deleteType(id);
    }
}
