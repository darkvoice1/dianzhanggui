package com.darkvoice1.dianzhanggui.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 配置项目的 OpenAPI 接口文档基本信息。 */
@Configuration
public class OpenApiConfig {

    /** 创建用于展示项目接口信息的 OpenAPI 配置。 */
    @Bean
    public OpenAPI dianZhangGuiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("店掌柜 API")
                        .description("商家经营 SaaS 服务端接口文档")
                        .version("v1"));
    }
}
