package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.Review;
import com.tom.wardrobe.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReviewService {

    @Resource
    private ReviewMapper reviewMapper;

    public List<Review> findByClothId(Integer clothId) {
        return reviewMapper.findByClothIdWithUserAndCloth(clothId);
    }

    public List<Review> findAll() {
        return reviewMapper.findAllWithUserAndCloth();
    }

    public Review findById(Integer id) {
        return reviewMapper.selectById(id);
    }

    public String addReview(Review review) {
        review.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        int count = reviewMapper.insert(review);
        return count > 0 ? "评论成功！" : "评论失败！";
    }

    public String deleteReview(Integer id) {
        int count = reviewMapper.deleteById(id);
        return count > 0 ? "删除成功！" : "删除失败！";
    }
}