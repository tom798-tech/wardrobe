package com.tom.wardrobe.controller;

import com.tom.wardrobe.entity.Review;
import com.tom.wardrobe.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/review")
public class ReviewController {

    @Resource
    private ReviewService reviewService;

    @GetMapping("/cloth/{clothId}")
    public List<Review> getReviewsByClothId(@PathVariable Integer clothId) {
        return reviewService.findByClothId(clothId);
    }

    @GetMapping
    public List<Review> getAllReviews() {
        return reviewService.findAll();
    }

    @GetMapping("/{id}")
    public Review getReviewById(@PathVariable Integer id) {
        return reviewService.findById(id);
    }

    @PostMapping
    public String addReview(@RequestBody Review review) {
        return reviewService.addReview(review);
    }

    @DeleteMapping("/{id}")
    public String deleteReview(@PathVariable Integer id) {
        return reviewService.deleteReview(id);
    }
}