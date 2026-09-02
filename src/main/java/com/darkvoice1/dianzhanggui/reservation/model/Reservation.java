package com.darkvoice1.dianzhanggui.reservation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示客户对一次商品可用性资源的预约记录。 */
@TableName("reservation")
public class Reservation {

    /** 预约记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家主键。 */
    private Long merchantId;

    /** 商品可用性记录主键。 */
    private Long productAvailabilityId;

    /** 客户档案主键。 */
    private Long customerProfileId;

    /** 预约状态。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 取消时间。 */
    private LocalDateTime cancelledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getProductAvailabilityId() { return productAvailabilityId; }
    public void setProductAvailabilityId(Long productAvailabilityId) { this.productAvailabilityId = productAvailabilityId; }
    public Long getCustomerProfileId() { return customerProfileId; }
    public void setCustomerProfileId(Long customerProfileId) { this.customerProfileId = customerProfileId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
