package com.darkvoice1.dianzhanggui.reservation.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 表示客户创建预约时提交的商品可用性记录。 */
public record CreateReservationRequest(
        @NotNull(message = "商品可用性 ID 不能为空")
        @Positive(message = "商品可用性 ID 必须是正整数")
        Long productAvailabilityId) {
}
