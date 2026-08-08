package com.tom.wardrobe.config;

import com.tom.wardrobe.interceptor.IdempotentInterceptor;
import com.tom.wardrobe.interceptor.RateLimitInterceptor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final IdempotentInterceptor idempotentInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor, IdempotentInterceptor idempotentInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.idempotentInterceptor = idempotentInterceptor;
    }

    /**
     * 动态上传目录：
     *   - 开发环境 (IDEA 直接跑): 默认 src/main/resources/images
     *   - Docker 环境: 环境变量 WARDROBE_UPLOAD_DIR=/app/images
     */
    @Value("${wardrobe.upload.dir:src/main/resources/images}")
    private String uploadDir;

    @PostConstruct
    public void ensureUploadDir() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. jar 内自带的 demo 图片 (classpath:/images/)
        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        "classpath:/images/",
                        "file:" + new File(uploadDir).getAbsolutePath() + File.separator
                );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册限流拦截器（对所有请求生效）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**");

        // 注册幂等性拦截器（对所有请求生效，仅拦截带有 @Idempotent 注解的方法）
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/**");
    }
}
