package com.tom.wardrobe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置类
 * 提供密码加密器 Bean
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt 密码加密器
     * BCrypt 自带盐值，安全性高
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}