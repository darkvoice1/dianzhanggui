package com.darkvoice1.dianzhanggui.catalog.controller;

import com.darkvoice1.dianzhanggui.catalog.model.CreateProductServiceRequest;
import com.darkvoice1.dianzhanggui.catalog.model.ProductService;
import com.darkvoice1.dianzhanggui.catalog.model.UpdateProductServiceRequest;
import com.darkvoice1.dianzhanggui.catalog.service.ProductServiceCatalogService;
import com.darkvoice1.dianzhanggui.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
    public ApiResponse<ProductService> create(@Valid @RequestBody CreateProductServiceRequest request) {
        return ApiResponse.success(productServiceCatalogService.create(request));
    }

    /** 查询当前商家的商品或服务详情。 */
    @GetMapping("/{id}")
    public ApiResponse<ProductService> findById(@PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productServiceCatalogService.findById(id));
    }

    /** 编辑当前商家的商品或服务。 */
    @PatchMapping("/{id}")
    public ApiResponse<ProductService> update(@PathVariable @Positive(message = "id 必须是正整数") Long id,
            @Valid @RequestBody UpdateProductServiceRequest request) {
        return ApiResponse.success(productServiceCatalogService.update(id, request));
    }

    /** 将当前商家的商品或服务上架。 */
    @PostMapping("/{id}/publish")
    public ApiResponse<ProductService> publish(@PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productServiceCatalogService.publish(id));
    }

    /** 将当前商家的商品或服务下架。 */
    @PostMapping("/{id}/unpublish")
    public ApiResponse<ProductService> unpublish(@PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(productServiceCatalogService.unpublish(id));
    }
}
