package com.darkvoice1.dianzhanggui.tenant.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示一个商家，同时作为多租户的数据边界。 */
@TableName("merchant")
public class Merchant {

    /** 商家主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家名称。 */
    private String name;

    /** 商家创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
