package com.darkvoice1.dianzhanggui.customer.controller;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.customer.model.CreateCustomerProfileRequest;
import com.darkvoice1.dianzhanggui.customer.model.CustomerProfile;
import com.darkvoice1.dianzhanggui.customer.service.CustomerProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供当前商家客户档案的新增和查询接口。 */
@RestController
@RequestMapping("/api/customer-profiles")
@Validated
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    /** 创建客户档案控制器并注入客户档案服务。 */
    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    /** 在当前商家创建客户档案。 */
    @PostMapping
    public ApiResponse<CustomerProfile> create(@Valid @RequestBody CreateCustomerProfileRequest request) {
        return ApiResponse.success(customerProfileService.create(request));
    }

    /** 查询当前商家的客户档案详情。 */
    @GetMapping("/{id}")
    public ApiResponse<CustomerProfile> findById(
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(customerProfileService.findById(id));
    }
}
