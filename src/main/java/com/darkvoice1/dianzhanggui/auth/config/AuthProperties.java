package com.darkvoice1.dianzhanggui.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** 保存 JWT 和刷新令牌的基础配置。 */
@ConfigurationProperties(prefix = "auth.jwt")
public record AuthProperties(
        String issuer,
        String secret,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration) {
}
