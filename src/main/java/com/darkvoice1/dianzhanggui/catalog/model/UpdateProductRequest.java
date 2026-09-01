package com.darkvoice1.dianzhanggui.catalog.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 表示编辑商品或服务时提交的信息。 */
public record UpdateProductRequest(
        @NotBlank(message = "商品或服务名称不能为空")
        @Size(max = 120, message = "商品或服务名称不能超过 120 个字符")
        String name,
        @NotBlank(message = "商品或服务类型不能为空")
        String type,
        @Size(max = 500, message = "商品或服务描述不能超过 500 个字符")
        String description,
        @NotNull(message = "原价不能为空")
        @DecimalMin(value = "0.00", message = "原价不能小于 0")
        @Digits(integer = 10, fraction = 2, message = "原价最多保留两位小数")
        BigDecimal originalPrice,
        @NotNull(message = "销售价不能为空")
        @DecimalMin(value = "0.00", message = "销售价不能小于 0")
        @Digits(integer = 10, fraction = 2, message = "销售价最多保留两位小数")
        BigDecimal sellingPrice) {
}
