package com.darkvoice1.dianzhanggui.tenant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 表示老板变更商家成员角色时提交的信息。 */
public record ChangeMerchantMemberRoleRequest(
        @NotBlank(message = "目标角色不能为空")
        @Pattern(regexp = "MEMBER|EMPLOYEE", message = "目标角色只能是 MEMBER 或 EMPLOYEE")
        String role) {
}
