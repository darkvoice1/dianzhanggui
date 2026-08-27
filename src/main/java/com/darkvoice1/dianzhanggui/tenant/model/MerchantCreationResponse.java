package com.darkvoice1.dianzhanggui.tenant.model;

/** 表示创建商家及首个门店后的结果数据。 */
public record MerchantCreationResponse(
        Long merchantId,
        String merchantName,
        Long firstStoreId,
        String firstStoreName) {
}
