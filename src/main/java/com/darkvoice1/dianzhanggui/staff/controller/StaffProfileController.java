package com.darkvoice1.dianzhanggui.staff.controller;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.staff.model.CreateStaffProfileRequest;
import com.darkvoice1.dianzhanggui.staff.model.StaffProfile;
import com.darkvoice1.dianzhanggui.staff.model.UpdateStaffProfileRequest;
import com.darkvoice1.dianzhanggui.staff.service.StaffProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供当前商家人员档案的新增、查询、编辑和停用接口。 */
@RestController
@RequestMapping("/api/staff-profiles")
@Validated
public class StaffProfileController {

    private final StaffProfileService staffProfileService;

    /** 创建人员档案控制器并注入人员档案服务。 */
    public StaffProfileController(StaffProfileService staffProfileService) {
        this.staffProfileService = staffProfileService;
    }

    /** 在当前商家创建员工人员档案。 */
    @PostMapping
    public ApiResponse<StaffProfile> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateStaffProfileRequest request) {
        return ApiResponse.success(staffProfileService.create(currentUserId(jwt), request));
    }

    /** 编辑当前商家的人员档案。 */
    @PatchMapping("/{id}")
    public ApiResponse<StaffProfile> update(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id,
            @Valid @RequestBody UpdateStaffProfileRequest request) {
        return ApiResponse.success(staffProfileService.update(currentUserId(jwt), id, request));
    }

    /** 停用当前商家的人员档案。 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        staffProfileService.deactivate(currentUserId(jwt), id);
        return ApiResponse.success(null);
    }

    /** 查询当前商家的人员档案详情。 */
    @GetMapping("/{id}")
    public ApiResponse<StaffProfile> findById(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(staffProfileService.findById(currentUserId(jwt), id));
    }

    /** 从已验证的 JWT 中读取当前操作者 ID。 */
    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
