package com.darkvoice1.dianzhanggui.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 表示用户登录时提交的邮箱和密码。 */
public record LoginRequest(
        @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
        @NotBlank(message = "密码不能为空") @Size(max = 64, message = "密码长度不能超过 64 个字符") String password) {
}
