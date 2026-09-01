package com.darkvoice1.dianzhanggui.catalog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.darkvoice1.dianzhanggui.catalog.mapper.ProductMapper;
import com.darkvoice1.dianzhanggui.catalog.model.CreateProductRequest;
import com.darkvoice1.dianzhanggui.catalog.model.Product;
import com.darkvoice1.dianzhanggui.catalog.model.ProductQuery;
import com.darkvoice1.dianzhanggui.catalog.model.UpdateProductRequest;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.page.PageResult;
import com.darkvoice1.dianzhanggui.common.page.PageResults;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.permission.service.PermissionResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 提供当前商家商品与服务目录的管理能力。 */
@Service
public class ProductCatalogService {

    private static final String PRODUCT_TYPE = "PRODUCT";
    private static final String SERVICE_TYPE = "SERVICE";
    private static final String DRAFT_STATUS = "DRAFT";
    private static final String ON_SALE_STATUS = "ON_SALE";
    private static final String OFF_SALE_STATUS = "OFF_SALE";
    private static final String CATALOG_MANAGE_PERMISSION = "CATALOG_MANAGE";

    private final ProductMapper productMapper;
    private final PermissionResolver permissionResolver;

    /** 创建商品与服务目录服务并注入数据访问组件。 */
    public ProductCatalogService(ProductMapper productMapper,
            PermissionResolver permissionResolver) {
        this.productMapper = productMapper;
        this.permissionResolver = permissionResolver;
    }

    /** 在当前商家创建商品或服务，初始状态为草稿。 */
    public Product create(Long operatorUserId, CreateProductRequest request) {
        requireCatalogPermission(operatorUserId);
        validateType(request.type());
        validatePrices(request.originalPrice(), request.sellingPrice());

        Product product = new Product();
        product.setMerchantId(TenantContext.requireMerchantId());
        product.setName(request.name().trim());
        product.setType(request.type().trim().toUpperCase());
        product.setDescription(normalize(request.description()));
        product.setOriginalPrice(request.originalPrice());
        product.setSellingPrice(request.sellingPrice());
        product.setStatus(DRAFT_STATUS);
        productMapper.insert(product);
        return product;
    }

    /** 查询当前商家的商品或服务详情。 */
    public Product findById(Long operatorUserId, Long id) {
        requireCatalogPermission(operatorUserId);
        return findCurrentMerchantProduct(id);
    }

    /** 编辑当前商家的商品或服务基础资料和价格。 */
    public Product update(Long operatorUserId, Long id, UpdateProductRequest request) {
        requireCatalogPermission(operatorUserId);
        validateType(request.type());
        validatePrices(request.originalPrice(), request.sellingPrice());

        Product product = findCurrentMerchantProduct(id);
        product.setName(request.name().trim());
        product.setType(request.type().trim().toUpperCase());
        product.setDescription(normalize(request.description()));
        product.setOriginalPrice(request.originalPrice());
        product.setSellingPrice(request.sellingPrice());
        productMapper.updateById(product);
        return product;
    }

    /** 将当前商家的草稿或下架目录设为上架。 */
    public Product publish(Long operatorUserId, Long id) {
        requireCatalogPermission(operatorUserId);
        Product product = findCurrentMerchantProduct(id);
        if (ON_SALE_STATUS.equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品或服务已经上架");
        }
        product.setStatus(ON_SALE_STATUS);
        productMapper.updateById(product);
        return product;
    }

    /** 将当前商家的上架目录设为下架。 */
    public Product unpublish(Long operatorUserId, Long id) {
        requireCatalogPermission(operatorUserId);
        Product product = findCurrentMerchantProduct(id);
        if (!ON_SALE_STATUS.equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有上架状态的商品或服务可以下架");
        }
        product.setStatus(OFF_SALE_STATUS);
        productMapper.updateById(product);
        return product;
    }

    /** 分页查询当前商家的商品与服务目录。 */
    public PageResult<Product> page(Long operatorUserId, ProductQuery query) {
        requireCatalogPermission(operatorUserId);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, TenantContext.requireMerchantId())
                .orderByDesc(Product::getId);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(Product::getName, query.getKeyword().trim());
        }
        if (query.getType() != null && !query.getType().isBlank()) {
            wrapper.eq(Product::getType, query.getType().trim().toUpperCase());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(Product::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (query.getCreatedFrom() != null) {
            wrapper.ge(Product::getCreatedAt, query.getCreatedFrom());
        }
        if (query.getCreatedTo() != null) {
            wrapper.le(Product::getCreatedAt, query.getCreatedTo());
        }
        return PageResults.from(productMapper.selectPage(
                new Page<>(query.currentPage(), query.getSize()), wrapper));
    }

    /** 按当前租户和主键查询目录，避免跨商家访问。 */
    private Product findCurrentMerchantProduct(Long id) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, id)
                .eq(Product::getMerchantId, TenantContext.requireMerchantId()));
        if (product == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return product;
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
