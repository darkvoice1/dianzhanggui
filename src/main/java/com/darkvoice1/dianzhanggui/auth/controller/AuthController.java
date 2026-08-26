package com.darkvoice1.dianzhanggui.auth.controller;

import com.darkvoice1.dianzhanggui.auth.model.LoginRequest;
import com.darkvoice1.dianzhanggui.auth.model.RefreshTokenRequest;
import com.darkvoice1.dianzhanggui.auth.model.RegisterRequest;
import com.darkvoice1.dianzhanggui.auth.model.TokenResponse;
import com.darkvoice1.dianzhanggui.auth.service.AuthService;
import com.darkvoice1.dianzhanggui.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供用户注册、登录和会话管理接口。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /** 创建认证控制器并注入认证服务。 */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 注册用户并返回首次登录令牌。 */
    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /** 校验用户凭证并返回登录令牌。 */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /** 使用刷新令牌换取新的一组令牌。 */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    /** 注销刷新令牌，终止后续刷新会话。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.success(null);
    }
}
