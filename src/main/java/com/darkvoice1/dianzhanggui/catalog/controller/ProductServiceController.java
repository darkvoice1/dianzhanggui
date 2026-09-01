package com.darkvoice1.dianzhanggui.catalog.controller;

import com.darkvoice1.dianzhanggui.catalog.model.CreateProductServiceRequest;
import com.darkvoice1.dianzhanggui.catalog.model.ProductService;
import com.darkvoice1.dianzhanggui.catalog.model.UpdateProductServiceRequest;
import com.darkvoice1.dianzhanggui.catalog.service.ProductServiceCatalogService;
import com.darkvoice1.dianzhanggui.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供当前商家商品与服务目录的管理接口。 */
@RestController
@RequestMapping("/api/product-services")
@Validated
public class ProductServiceController {

    private final ProductServiceCatalogService productServiceCatalogService;

    /** 创建商品与服务目录控制器并注入目录服务。 */
    public ProductServiceController(ProductServiceCatalogService productServiceCatalogService) {
        this.productServiceCatalogService = productServiceCatalogService;
    }

    /** 在当前商家创建商品或服务。 */
    @PostMapping
    public ApiResponse<ProductService> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProductServiceRequest request) {
        return ApiResponse.success(productServiceCatalogService.create(currentUserId(jwt), request));
    }

    /** 查询当前商家的商品或服务详情。 */
    @GetMapping("/{id}")
    public ApiResponse<ProductService> findById(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productServiceCatalogService.findById(currentUserId(jwt), id));
    }

    /** 编辑当前商家的商品或服务。 */
    @PatchMapping("/{id}")
    public ApiResponse<ProductService> update(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id,
            @Valid @RequestBody UpdateProductServiceRequest request) {
        return ApiResponse.success(productServiceCatalogService.update(currentUserId(jwt), id, request));
    }

    /** 将当前商家的商品或服务上架。 */
    @PostMapping("/{id}/publish")
    public ApiResponse<ProductService> publish(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productServiceCatalogService.publish(currentUserId(jwt), id));
    }

    /** 将当前商家的商品或服务下架。 */
    @PostMapping("/{id}/unpublish")
    public ApiResponse<ProductService> unpublish(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productServiceCatalogService.unpublish(currentUserId(jwt), id));
    }

    /** 从已验证的 JWT 中读取当前操作者 ID。 */
    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
