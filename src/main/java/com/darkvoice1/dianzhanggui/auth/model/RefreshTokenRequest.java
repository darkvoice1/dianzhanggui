package com.darkvoice1.dianzhanggui.auth.model;

import jakarta.validation.constraints.NotBlank;

/** 表示刷新访问令牌时提交的刷新令牌。 */
public record RefreshTokenRequest(@NotBlank(message = "刷新令牌不能为空") String refreshToken) {
}
