package com.darkvoice1.dianzhanggui.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 表示用户注册时提交的邮箱和密码。 */
public record RegisterRequest(
        @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
        @NotBlank(message = "密码不能为空") @Size(min = 8, max = 64, message = "密码长度必须在 8 到 64 个字符之间") String password) {
}
