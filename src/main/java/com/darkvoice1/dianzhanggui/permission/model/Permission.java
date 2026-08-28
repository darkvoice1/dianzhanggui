package com.darkvoice1.dianzhanggui.permission.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示系统中可用于接口鉴权的权限定义。 */
@TableName("permission")
public class Permission {

    /** 权限主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 稳定的权限编码。 */
    private String code;

    /** 权限显示名称。 */
    private String name;

    /** 权限说明。 */
    private String description;

    /** 权限创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
