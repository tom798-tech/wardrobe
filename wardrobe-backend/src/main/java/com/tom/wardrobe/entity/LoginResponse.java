package com.tom.wardrobe.entity;

/**
 * 登录响应 DTO
 * 不包含敏感信息（如密码），只返回必要字段
 */
public class LoginResponse {

    private Integer id;
    private String userName;
    private String phone;
    private String address;
    private Integer role;
    private String token;

    public LoginResponse() {
    }

    /**
     * 从 User 实体构建响应对象（自动剔除敏感字段）
     */
    public static LoginResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUserName(user.getUserName());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole());
        response.setToken(user.getToken());
        // 密码字段不包含在内
        return response;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}