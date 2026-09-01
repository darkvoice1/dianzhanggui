package com.darkvoice1.dianzhanggui.tenant.controller;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.common.page.PageResult;
import com.darkvoice1.dianzhanggui.tenant.model.ChangeMerchantMemberRoleRequest;
import com.darkvoice1.dianzhanggui.tenant.model.CreateMerchantRequest;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantCreationResponse;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantSummaryResponse;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMemberQuery;
import com.darkvoice1.dianzhanggui.tenant.service.MerchantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 提供商家创建、加入和切换相关接口。 */
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

    /** 将当前登录用户以顾客身份加入指定商家。 */
    @PostMapping("/{merchantId}/members")
    public ApiResponse<Void> joinMerchant(@AuthenticationPrincipal Jwt jwt, @PathVariable Long merchantId) {
        merchantService.joinMerchant(Long.valueOf(jwt.getSubject()), merchantId);
        return ApiResponse.success(null);
    }

    /** 分页查询当前商家的成员关系。 */
    @GetMapping("/{merchantId}/members")
    public ApiResponse<PageResult<MerchantMember>> pageMembers(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "merchantId 必须是正整数") Long merchantId,
            @Valid MerchantMemberQuery query) {
        return ApiResponse.success(merchantService.pageMembers(Long.valueOf(jwt.getSubject()), merchantId, query));
    }

    /** 由老板变更当前商家已有成员的角色。 */
    @PatchMapping("/{merchantId}/members/{memberUserId}/role")
    public ApiResponse<Void> changeMemberRole(@AuthenticationPrincipal Jwt jwt, @PathVariable Long merchantId,
            @PathVariable Long memberUserId, @Valid @RequestBody ChangeMerchantMemberRoleRequest request) {
        merchantService.changeMemberRole(Long.valueOf(jwt.getSubject()), merchantId, memberUserId, request);
        return ApiResponse.success(null);
    }

    /** 查询当前登录用户所属的全部商家。 */
    @GetMapping("/mine")
    public ApiResponse<List<MerchantSummaryResponse>> getMyMerchants(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(merchantService.getMyMerchants(Long.valueOf(jwt.getSubject())));
    }

    /** 校验当前登录用户是否可以切换到指定商家。 */
    @PostMapping("/{merchantId}/switch")
    public ApiResponse<MerchantSummaryResponse> switchMerchant(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long merchantId) {
        return ApiResponse.success(merchantService.switchMerchant(Long.valueOf(jwt.getSubject()), merchantId));
    }
}
