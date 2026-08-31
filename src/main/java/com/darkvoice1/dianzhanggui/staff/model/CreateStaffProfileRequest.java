package com.darkvoice1.dianzhanggui.staff.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 表示创建商家人员档案时提交的信息。 */
public record CreateStaffProfileRequest(
        @NotNull(message = "用户 ID 不能为空")
        @Positive(message = "用户 ID 必须是正整数")
        Long userId,
        @NotBlank(message = "人员姓名不能为空")
        @Size(max = 120, message = "人员姓名不能超过 120 个字符")
        String name,
        @Size(max = 32, message = "人员电话不能超过 32 个字符")
        String phone,
        @Size(max = 60, message = "岗位名称不能超过 60 个字符")
        String position) {
}
