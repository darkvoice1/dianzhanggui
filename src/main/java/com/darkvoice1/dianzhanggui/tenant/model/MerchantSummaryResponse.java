package com.darkvoice1.dianzhanggui.tenant.model;

/** 表示当前用户所属商家的简要信息。 */
public record MerchantSummaryResponse(
        Long merchantId,
        String merchantName,
        String role) {
}
