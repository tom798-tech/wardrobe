package com.tom.wardrobe.service;

import cn.dev33.satoken.stp.StpUtil;
import com.tom.wardrobe.entity.User;
import com.tom.wardrobe.entity.UserVo;
import com.tom.wardrobe.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    public List<User> getUserByParam(String queryStr) {
        if (queryStr != null && !queryStr.isEmpty()) {
            return userMapper.getUserByParam(queryStr);
        }
        return userMapper.getAllUsers();
    }

    public String delUser(Integer id) {
        int count = userMapper.deleteById(id);
        return count > 0 ? "删除成功！" : "删除失败！";
    }

    public User getCurrentUser(Integer id) {
        return userMapper.selectById(id);
    }

    public String updateUser(UserVo userVo) {
        User currentUser = this.getCurrentUser(userVo.getId());
        // 使用 BCrypt 验证密码
        if (!passwordEncoder.matches(userVo.getPassword(), currentUser.getPassword())) {
            return "原始密码输入错误，请重新输入！";
        }
        User userNameExist = userMapper.findByUserName(userVo.getUserName());
        User phoneExist = userMapper.findByPhone(userVo.getPhone());
        if (userNameExist != null && !userNameExist.getUserName().equals(userVo.getUserName())) {
            return "用户名已存在，请输入其他用户名！";
        }
        if (phoneExist != null && !phoneExist.getPhone().equals(userVo.getPhone())) {
            return "手机号已存在，请确认！";
        }
        // 加密新密码
        userVo.setPassword(passwordEncoder.encode(userVo.getNewpsw()));
        int updateCount = userMapper.updateById(userVo);
        return updateCount > 0 ? "修改成功！" : "修改失败！";
    }

    public String register(User user) {
        if (userMapper.findByUserName(user.getUserName()) != null) {
            return "用户名已存在，请更换！";
        }
        if (userMapper.findByPhone(user.getPhone()) != null) {
            return "手机号已存在，请确认！";
        }
        user.setRole(2);
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return "注册成功，请登录！";
    }

    public String addUser(User user) {
        if (userMapper.findByUserName(user.getUserName()) != null) {
            return "用户名已存在，请更换！";
        }
        if (userMapper.findByPhone(user.getPhone()) != null) {
            return "手机号已存在，请确认！";
        }
        if (user.getRole() == null) {
            user.setRole(2);
        }
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        int count = userMapper.insert(user);
        return count > 0 ? "添加成功！" : "添加失败！";
    }

    public String updateUserByAdmin(User user) {
        User exist = userMapper.selectById(user.getId());
        if (exist == null) {
            return "用户不存在！";
        }
        User userNameExist = userMapper.findByUserName(user.getUserName());
        User phoneExist = userMapper.findByPhone(user.getPhone());
        if (userNameExist != null && !userNameExist.getId().equals(user.getId())) {
            return "用户名已存在，请输入其他用户名！";
        }
        if (phoneExist != null && !phoneExist.getId().equals(user.getId())) {
            return "手机号已存在，请确认！";
        }
        // 如果密码有更新，加密新密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        int count = userMapper.updateById(user);
        return count > 0 ? "修改成功！" : "修改失败！";
    }

    public User login(String userInfo, String password) {
        // 先按用户名查找
        User user = userMapper.findByUserName(userInfo);
        if (user == null) {
            // 再按手机号查找
            user = userMapper.findByPhone(userInfo);
        }
        if (user != null) {
            // 使用 BCrypt 验证密码
            if (passwordEncoder.matches(password, user.getPassword())) {
                StpUtil.login(user.getId());
                user.setToken(StpUtil.getTokenValue());
                return user;
            }
        }
        return null;
    }
}