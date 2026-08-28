package com.darkvoice1.dianzhanggui.role.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示系统中可分配给商家成员的角色定义。 */
@TableName("role")
public class Role {

    /** 角色主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 稳定的角色编码。 */
    private String code;

    /** 角色显示名称。 */
    private String name;

    /** 角色说明。 */
    private String description;

    /** 角色创建时间。 */
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
