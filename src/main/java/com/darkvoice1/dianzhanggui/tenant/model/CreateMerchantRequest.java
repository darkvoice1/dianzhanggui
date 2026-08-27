package com.darkvoice1.dianzhanggui.tenant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 表示创建商家和首个门店时提交的请求数据。 */
public record CreateMerchantRequest(
        @NotBlank(message = "商家名称不能为空")
        @Size(max = 120, message = "商家名称不能超过 120 个字符")
        String merchantName,

        @NotBlank(message = "门店名称不能为空")
        @Size(max = 120, message = "门店名称不能超过 120 个字符")
        String firstStoreName,

        @Size(max = 255, message = "门店地址不能超过 255 个字符")
        String firstStoreAddress) {
}
