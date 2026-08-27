package com.darkvoice1.dianzhanggui.tenant.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示用户加入商家的成员关系。 */
@TableName("merchant_member")
public class MerchantMember {

    /** 成员关系主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家主键。 */
    private Long merchantId;

    /** 用户主键。 */
    private Long userId;

    /** 成员角色，具体权限由后续任务定义。 */
    private String role;

    /** 成员关系创建时间。 */
    private LocalDateTime createdAt;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
