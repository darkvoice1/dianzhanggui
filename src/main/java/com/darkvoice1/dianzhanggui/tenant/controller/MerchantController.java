package com.darkvoice1.dianzhanggui.tenant.controller;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.tenant.model.CreateMerchantRequest;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantCreationResponse;
import com.darkvoice1.dianzhanggui.tenant.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供商家创建相关接口。 */
@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    /** 创建商家控制器并注入商家服务。 */
    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /** 为当前登录用户创建商家和首个门店。 */
    @PostMapping
    public ApiResponse<MerchantCreationResponse> createMerchant(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMerchantRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ApiResponse.success(merchantService.createMerchant(userId, request));
    }
}
