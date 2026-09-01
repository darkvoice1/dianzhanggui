package com.darkvoice1.dianzhanggui.staff.model;

import com.darkvoice1.dianzhanggui.common.page.PageQuery;

import java.time.LocalDateTime;

/** 表示商家成员档案的分页筛选条件。 */
public class StaffProfileQuery extends PageQuery {

    private String keyword;
    private String status;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
