package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.Size;
import com.tom.wardrobe.mapper.SizeMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class SizeService {

    @Resource
    private SizeMapper sizeMapper;

    public List<Size> findAll() {
        return sizeMapper.selectList(null);
    }

    public List<Size> findByTypeId(Integer typeId) {
        return sizeMapper.findByTypeId(typeId);
    }

    public Size findById(Integer id) {
        return sizeMapper.selectById(id);
    }

    public String addSize(Size size) {
        int count = sizeMapper.insert(size);
        return count > 0 ? "添加成功！" : "添加失败！";
    }

    public String updateSize(Size size) {
        int count = sizeMapper.updateById(size);
        return count > 0 ? "修改成功！" : "修改失败！";
    }

    public String deleteSize(Integer id) {
        int count = sizeMapper.deleteById(id);
        return count > 0 ? "删除成功！" : "删除失败！";
    }
}