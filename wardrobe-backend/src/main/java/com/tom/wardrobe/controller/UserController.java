package com.tom.wardrobe.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.tom.wardrobe.entity.User;
import com.tom.wardrobe.entity.UserVo;
import com.tom.wardrobe.service.UserService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 获取用户列表（管理员专用）
     */
    @SaCheckRole("admin")
    @GetMapping
    public List<User> getUserByParam(@RequestParam(required = false) String queryStr) {
        return userService.getUserByParam(queryStr);
    }

    /**
     * 获取当前登录用户的信息（修复水平越权）
     */
    @GetMapping("/info")
    public User getCurrentUserInfo() {
        Integer userId = StpUtil.getLoginIdAsInt();
        User user = userService.getCurrentUser(userId);
        // 清除敏感信息
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 获取指定用户信息（管理员专用）
     */
    @SaCheckRole("admin")
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Integer id) {
        User user = userService.getCurrentUser(id);
        // 清除敏感信息
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 添加用户（管理员专用）
     */
    @SaCheckRole("admin")
    @PostMapping
    public String addUser(@RequestBody User user) {
        return userService.addUser(user);
    }

    /**
     * 管理员更新用户信息
     */
    @SaCheckRole("admin")
    @PutMapping("/updateByAdmin")
    public String updateUserByAdmin(@RequestBody User user) {
        return userService.updateUserByAdmin(user);
    }

    /**
     * 用户更新自己的信息（修复水平越权）
     */
    @PutMapping
    public String updateUser(@RequestBody UserVo userVo) {
        // 强制使用当前登录用户 ID
        userVo.setId(StpUtil.getLoginIdAsInt());
        return userService.updateUser(userVo);
    }

    /**
     * 删除用户（管理员专用）
     */
    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Integer id) {
        return userService.delUser(id);
    }
}