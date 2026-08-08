package com.tom.wardrobe.config;

import cn.dev33.satoken.stp.StpInterface;
import com.tom.wardrobe.entity.User;
import com.tom.wardrobe.mapper.UserMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 角色权限接口实现
 * 用于 @SaCheckRole 和 @SaCheckPermission 注解的权限验证
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserMapper userMapper;

    /**
     * 返回指定账号所拥有的角色列表
     * role=1 为管理员，role=2 为普通用户
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roleList = new ArrayList<>();
        try {
            Integer userId = null;
            if (loginId instanceof Integer) {
                userId = (Integer) loginId;
            } else if (loginId instanceof String) {
                userId = Integer.parseInt((String) loginId);
            }
            User user = userMapper.selectById(userId);
            if (user != null && user.getRole() != null) {
                if (user.getRole() == 1) {
                    roleList.add("admin");
                } else {
                    roleList.add("user");
                }
            }
        } catch (Exception e) {
            // 查询失败时返回空列表
        }
        return roleList;
    }

    /**
     * 返回指定账号所拥有的权限列表
     * 当前项目暂不使用细粒度权限，返回空列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}