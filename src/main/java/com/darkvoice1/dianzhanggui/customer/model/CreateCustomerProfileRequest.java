package com.darkvoice1.dianzhanggui.customer.model;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 表示创建客户档案时提交的信息。 */
public record CreateCustomerProfileRequest(
        @jakarta.validation.constraints.NotBlank(message = "客户姓名不能为空")
        @Size(max = 120, message = "客户姓名不能超过 120 个字符")
        String name,
        @Size(max = 32, message = "客户电话不能超过 32 个字符")
        String phone,
        @Positive(message = "用户 ID 必须是正整数")
        Long userId) {
}
