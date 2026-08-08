package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.Type;
import com.tom.wardrobe.mapper.TypeMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class TypeService {

    @Resource
    private TypeMapper typeMapper;

    public List<Type> findAll() {
        return typeMapper.selectList(null);
    }

    public Type findById(Integer id) {
        return typeMapper.selectById(id);
    }

    public String addType(Type type) {
        int count = typeMapper.insert(type);
        return count > 0 ? "添加成功！" : "添加失败！";
    }

    public String updateType(Type type) {
        int count = typeMapper.updateById(type);
        return count > 0 ? "修改成功！" : "修改失败！";
    }

    public String deleteType(Integer id) {
        int count = typeMapper.deleteById(id);
        return count > 0 ? "删除成功！" : "删除失败！";
    }
}