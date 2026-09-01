package com.darkvoice1.dianzhanggui.tenant.model;

import com.darkvoice1.dianzhanggui.common.page.PageQuery;

import java.time.LocalDateTime;

/** 表示商家成员关系的分页筛选条件。 */
public class MerchantMemberQuery extends PageQuery {

    private String role;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(LocalDateTime createdFrom) {
        this.createdFrom = createdFrom;
    }

    public LocalDateTime getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(LocalDateTime createdTo) {
        this.createdTo = createdTo;
    }
}
