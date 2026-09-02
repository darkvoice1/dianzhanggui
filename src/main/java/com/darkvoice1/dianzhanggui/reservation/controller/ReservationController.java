package com.darkvoice1.dianzhanggui.reservation.controller;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.reservation.model.CreateReservationRequest;
import com.darkvoice1.dianzhanggui.reservation.model.Reservation;
import com.darkvoice1.dianzhanggui.reservation.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供当前商家的通用预约创建接口。 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    /** 创建预约控制器并注入预约业务组件。 */
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** 为当前登录用户创建一条商品可用性预约。 */
    @PostMapping
    public ApiResponse<Reservation> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReservationRequest request) {
        return ApiResponse.success(reservationService.create(Long.valueOf(jwt.getSubject()), request));
    }

    /** 取消当前登录用户尚未开始的商品预约。 */
    @DeleteMapping("/{id}")
    public ApiResponse<Reservation> cancel(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(reservationService.cancel(Long.valueOf(jwt.getSubject()), id));
    }
}
