package com.darkvoice1.dianzhanggui.common.tenant;

import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;

/** 保存当前 HTTP 请求已确认的商家标识。 */
public final class TenantContext {

    /** 前端传递当前商家标识使用的请求头名称。 */
    public static final String MERCHANT_ID_HEADER = "X-Merchant-Id";

    private static final ThreadLocal<Long> CURRENT_MERCHANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    /** 设置当前请求的商家标识。 */
    public static void setMerchantId(Long merchantId) {
        CURRENT_MERCHANT_ID.set(merchantId);
    }

    /** 获取当前请求的商家标识，缺失时拒绝访问租户业务数据。 */
    public static Long requireMerchantId() {
        Long merchantId = CURRENT_MERCHANT_ID.get();
        if (merchantId == null) {
            throw new BusinessException(ErrorCode.TENANT_REQUIRED);
        }
        return merchantId;
    }

    /** 清理当前请求的商家标识，避免线程复用时串用数据。 */
    public static void clear() {
        CURRENT_MERCHANT_ID.remove();
    }
}
