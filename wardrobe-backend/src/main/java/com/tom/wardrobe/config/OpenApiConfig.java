package com.tom.wardrobe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("云衣橱商城 API")
                        .version("1.0.0")
                        .description("云衣橱商城后端接口文档 - 支持用户端和管理端的完整电商功能，包含AI智能推荐、以文搜衣、相似推荐等特色功能")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@wardrobe.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地开发环境"),
                        new Server().url("http://localhost:7070/api").description("用户端代理"),
                        new Server().url("http://localhost:7087/api").description("管理端代理")
                ));
    }
}
