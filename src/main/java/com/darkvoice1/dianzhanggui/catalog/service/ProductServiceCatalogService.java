package com.darkvoice1.dianzhanggui.catalog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.catalog.mapper.ProductServiceMapper;
import com.darkvoice1.dianzhanggui.catalog.model.CreateProductServiceRequest;
import com.darkvoice1.dianzhanggui.catalog.model.ProductService;
import com.darkvoice1.dianzhanggui.catalog.model.UpdateProductServiceRequest;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.permission.service.PermissionResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 提供当前商家商品与服务目录的管理能力。 */
@Service
public class ProductServiceCatalogService {

    private static final String PRODUCT_TYPE = "PRODUCT";
    private static final String SERVICE_TYPE = "SERVICE";
    private static final String DRAFT_STATUS = "DRAFT";
    private static final String ON_SALE_STATUS = "ON_SALE";
    private static final String OFF_SALE_STATUS = "OFF_SALE";
    private static final String CATALOG_MANAGE_PERMISSION = "CATALOG_MANAGE";

    private final ProductServiceMapper productServiceMapper;
    private final PermissionResolver permissionResolver;

    /** 创建商品与服务目录服务并注入数据访问组件。 */
    public ProductServiceCatalogService(ProductServiceMapper productServiceMapper,
            PermissionResolver permissionResolver) {
        this.productServiceMapper = productServiceMapper;
        this.permissionResolver = permissionResolver;
    }

    /** 在当前商家创建商品或服务，初始状态为草稿。 */
    public ProductService create(Long operatorUserId, CreateProductServiceRequest request) {
        requireCatalogPermission(operatorUserId);
        validateType(request.type());
        validatePrices(request.originalPrice(), request.sellingPrice());

        ProductService productService = new ProductService();
        productService.setMerchantId(TenantContext.requireMerchantId());
        productService.setName(request.name().trim());
        productService.setType(request.type().trim().toUpperCase());
        productService.setDescription(normalize(request.description()));
        productService.setOriginalPrice(request.originalPrice());
        productService.setSellingPrice(request.sellingPrice());
        productService.setStatus(DRAFT_STATUS);
        productServiceMapper.insert(productService);
        return productService;
    }

    /** 查询当前商家的商品或服务详情。 */
    public ProductService findById(Long operatorUserId, Long id) {
        requireCatalogPermission(operatorUserId);
        return findCurrentMerchantProductService(id);
    }

    /** 编辑当前商家的商品或服务基础资料和价格。 */
    public ProductService update(Long operatorUserId, Long id, UpdateProductServiceRequest request) {
        requireCatalogPermission(operatorUserId);
        validateType(request.type());
        validatePrices(request.originalPrice(), request.sellingPrice());

        ProductService productService = findCurrentMerchantProductService(id);
        productService.setName(request.name().trim());
        productService.setType(request.type().trim().toUpperCase());
        productService.setDescription(normalize(request.description()));
        productService.setOriginalPrice(request.originalPrice());
        productService.setSellingPrice(request.sellingPrice());
        productServiceMapper.updateById(productService);
        return productService;
    }

    /** 将当前商家的草稿或下架目录设为上架。 */
    public ProductService publish(Long operatorUserId, Long id) {
        requireCatalogPermission(operatorUserId);
        ProductService productService = findCurrentMerchantProductService(id);
        if (ON_SALE_STATUS.equals(productService.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品或服务已经上架");
        }
        productService.setStatus(ON_SALE_STATUS);
        productServiceMapper.updateById(productService);
        return productService;
    }

    /** 将当前商家的上架目录设为下架。 */
    public ProductService unpublish(Long operatorUserId, Long id) {
        requireCatalogPermission(operatorUserId);
        ProductService productService = findCurrentMerchantProductService(id);
        if (!ON_SALE_STATUS.equals(productService.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有上架状态的商品或服务可以下架");
        }
        productService.setStatus(OFF_SALE_STATUS);
        productServiceMapper.updateById(productService);
        return productService;
    }

    /** 按当前租户和主键查询目录，避免跨商家访问。 */
    private ProductService findCurrentMerchantProductService(Long id) {
        ProductService productService = productServiceMapper.selectOne(new LambdaQueryWrapper<ProductService>()
                .eq(ProductService::getId, id)
                .eq(ProductService::getMerchantId, TenantContext.requireMerchantId()));
        if (productService == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return productService;
    }

    /** 校验操作者在当前商家拥有目录管理权限。 */
    private void requireCatalogPermission(Long operatorUserId) {
        permissionResolver.requirePermission(operatorUserId, CATALOG_MANAGE_PERMISSION);
    }

    /** 校验目录类型属于通用商品或服务。 */
    private void validateType(String type) {
        String normalizedType = type == null ? null : type.trim().toUpperCase();
        if (!PRODUCT_TYPE.equals(normalizedType) && !SERVICE_TYPE.equals(normalizedType)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品或服务类型只能是 PRODUCT 或 SERVICE");
        }
    }

    /** 校验原价和销售价的大小关系。 */
    private void validatePrices(BigDecimal originalPrice, BigDecimal sellingPrice) {
        if (originalPrice == null || sellingPrice == null
                || sellingPrice.compareTo(originalPrice) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "销售价不能高于原价");
        }
    }

    /** 将空白描述统一保存为空值。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
