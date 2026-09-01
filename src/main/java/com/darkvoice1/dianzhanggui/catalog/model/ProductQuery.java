package com.darkvoice1.dianzhanggui.catalog.model;

import com.darkvoice1.dianzhanggui.common.page.PageQuery;

import java.time.LocalDateTime;

/** 表示商品与服务目录的分页筛选条件。 */
public class ProductQuery extends PageQuery {

    private String keyword;
    private String type;
    private String status;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
