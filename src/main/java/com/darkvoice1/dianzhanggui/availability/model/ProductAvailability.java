package com.darkvoice1.dianzhanggui.availability.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示商品在指定时间范围内可被预约或提供的资源。 */
@TableName("product_availability")
public class ProductAvailability {

    /** 商品可用性记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家主键。 */
    private Long merchantId;

    /** 关联的商品主键。 */
    private Long productId;

    /** 可用开始时间。 */
    private LocalDateTime startAt;

    /** 可用结束时间。 */
    private LocalDateTime endAt;

    /** 可提供的总数量。 */
    private Integer capacity;

    /** 当前剩余数量。 */
    private Integer remainingCapacity;

    /** 商品可用性状态。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 最后修改时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getRemainingCapacity() { return remainingCapacity; }
    public void setRemainingCapacity(Integer remainingCapacity) { this.remainingCapacity = remainingCapacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
