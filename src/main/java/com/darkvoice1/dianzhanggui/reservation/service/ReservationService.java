package com.darkvoice1.dianzhanggui.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.availability.mapper.ProductAvailabilityMapper;
import com.darkvoice1.dianzhanggui.availability.model.ProductAvailability;
import com.darkvoice1.dianzhanggui.catalog.mapper.ProductMapper;
import com.darkvoice1.dianzhanggui.catalog.model.Product;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.customer.mapper.CustomerProfileMapper;
import com.darkvoice1.dianzhanggui.customer.model.CustomerProfile;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.reservation.mapper.ReservationMapper;
import com.darkvoice1.dianzhanggui.reservation.model.CreateReservationRequest;
import com.darkvoice1.dianzhanggui.reservation.model.Reservation;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 提供通用商品可用性预约的创建能力。 */
@Service
public class ReservationService {

    private static final String ON_SALE_STATUS = "ON_SALE";
    private static final String AVAILABILITY_OPEN_STATUS = "OPEN";
    private static final String RESERVATION_STATUS = "RESERVED";
    private static final String CUSTOMER_ACTIVE_STATUS = "ACTIVE";

    private final ProductAvailabilityMapper productAvailabilityMapper;
    private final ProductMapper productMapper;
    private final CustomerProfileMapper customerProfileMapper;
    private final ReservationMapper reservationMapper;

    /** 创建预约组件并注入预约、商品和客户档案数据访问组件。 */
    public ReservationService(ProductAvailabilityMapper productAvailabilityMapper, ProductMapper productMapper,
            CustomerProfileMapper customerProfileMapper, ReservationMapper reservationMapper) {
        this.productAvailabilityMapper = productAvailabilityMapper;
        this.productMapper = productMapper;
        this.customerProfileMapper = customerProfileMapper;
        this.reservationMapper = reservationMapper;
    }

    /** 为当前登录用户创建预约，客户和商家均从已验证上下文中确定。 */
    public Reservation create(Long userId, CreateReservationRequest request) {
        Long merchantId = TenantContext.requireMerchantId();
        ProductAvailability availability = findAvailability(merchantId, request.productAvailabilityId());
        validateAvailability(merchantId, availability);

        CustomerProfile customer = customerProfileMapper.selectOne(new LambdaQueryWrapper<CustomerProfile>()
                .eq(CustomerProfile::getMerchantId, merchantId)
                .eq(CustomerProfile::getUserId, userId)
                .eq(CustomerProfile::getStatus, CUSTOMER_ACTIVE_STATUS));
        if (customer == null) {
            throw new BusinessException(ErrorCode.RESERVATION_CUSTOMER_REQUIRED);
        }

        Reservation reservation = new Reservation();
        reservation.setMerchantId(merchantId);
        reservation.setProductAvailabilityId(availability.getId());
        reservation.setCustomerProfileId(customer.getId());
        reservation.setStatus(RESERVATION_STATUS);
        reservationMapper.insert(reservation);
        return reservation;
    }

    /** 按当前商家查询商品可用性记录，防止跨租户预约。 */
    private ProductAvailability findAvailability(Long merchantId, Long availabilityId) {
        ProductAvailability availability = productAvailabilityMapper.selectOne(
                new LambdaQueryWrapper<ProductAvailability>()
                        .eq(ProductAvailability::getId, availabilityId)
                        .eq(ProductAvailability::getMerchantId, merchantId));
        if (availability == null) {
            throw new BusinessException(ErrorCode.PRODUCT_AVAILABILITY_NOT_FOUND);
        }
        return availability;
    }

    /** 校验商品上架状态、可用性状态、开始时间和剩余数量。 */
    private void validateAvailability(Long merchantId, ProductAvailability availability) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, availability.getProductId())
                .eq(Product::getMerchantId, merchantId));
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_AVAILABILITY_NOT_FOUND);
        }
        if (!ON_SALE_STATUS.equals(product.getStatus())
                || !AVAILABILITY_OPEN_STATUS.equals(availability.getStatus())
                || !availability.getStartAt().isAfter(LocalDateTime.now())
                || availability.getRemainingCapacity() <= 0) {
            throw new BusinessException(ErrorCode.PRODUCT_AVAILABILITY_NOT_BOOKABLE);
        }
    }
}
