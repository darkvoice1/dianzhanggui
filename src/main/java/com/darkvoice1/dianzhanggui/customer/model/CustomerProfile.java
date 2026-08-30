package com.darkvoice1.dianzhanggui.customer.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示商家名下的客户档案，可关联系统登录用户或作为线下客户保存。 */
@TableName("customer_profile")
public class CustomerProfile {

    /** 客户档案主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家主键。 */
    private Long merchantId;

    /** 关联的系统用户主键，线下客户可以为空。 */
    private Long userId;

    /** 客户姓名。 */
    private String name;

    /** 客户联系电话。 */
    private String phone;

    /** 档案状态，例如 ACTIVE 或 INACTIVE。 */
    private String status;

    /** 档案创建时间。 */
    private LocalDateTime createdAt;

    /** 档案最后修改时间。 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
