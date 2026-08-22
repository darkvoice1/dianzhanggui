package com.darkvoice1.dianzhanggui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 店掌柜应用启动类。 */
@SpringBootApplication
public class DianZhangGuiApplication {

    /** 启动 Spring Boot 应用。 */
    public static void main(String[] args) {
        // 加载 Spring 配置并启动内嵌 Web 服务器。
        SpringApplication.run(DianZhangGuiApplication.class, args);
    }
}
