package com.darkvoice1.dianzhanggui.catalog.controller;

import com.darkvoice1.dianzhanggui.catalog.model.CreateProductRequest;
import com.darkvoice1.dianzhanggui.catalog.model.Product;
import com.darkvoice1.dianzhanggui.catalog.model.ProductQuery;
import com.darkvoice1.dianzhanggui.catalog.model.UpdateProductRequest;
import com.darkvoice1.dianzhanggui.catalog.service.ProductCatalogService;
import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.common.page.PageResult;
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
public class ProductController {

    private final ProductCatalogService productCatalogService;

    /** 创建商品与服务目录控制器并注入目录服务。 */
    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    /** 在当前商家创建商品或服务。 */
    @PostMapping
    public ApiResponse<Product> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success(productCatalogService.create(currentUserId(jwt), request));
    }

    /** 查询当前商家的商品或服务详情。 */
    @GetMapping("/{id}")
    public ApiResponse<Product> findById(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productCatalogService.findById(currentUserId(jwt), id));
    }

    /** 分页查询当前商家的商品与服务目录。 */
    @GetMapping
    public ApiResponse<PageResult<Product>> page(@AuthenticationPrincipal Jwt jwt,
            @Valid ProductQuery query) {
        return ApiResponse.success(productCatalogService.page(currentUserId(jwt), query));
    }

    /** 编辑当前商家的商品或服务。 */
    @PatchMapping("/{id}")
    public ApiResponse<Product> update(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(productCatalogService.update(currentUserId(jwt), id, request));
    }

    /** 将当前商家的商品或服务上架。 */
    @PostMapping("/{id}/publish")
    public ApiResponse<Product> publish(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productCatalogService.publish(currentUserId(jwt), id));
    }

    /** 将当前商家的商品或服务下架。 */
    @PostMapping("/{id}/unpublish")
    public ApiResponse<Product> unpublish(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productCatalogService.unpublish(currentUserId(jwt), id));
    }

    /** 从已验证的 JWT 中读取当前操作者 ID。 */
    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
