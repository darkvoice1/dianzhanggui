package com.darkvoice1.dianzhanggui.auth.model;

/** 表示登录或刷新成功后返回的一组令牌。 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
