package com.darkvoice1.dianzhanggui.auth.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示可登录店掌柜系统的用户账号。 */
@TableName("app_user")
public class UserAccount {

    /** 用户主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户登录邮箱。 */
    private String email;

    /** 使用 BCrypt 保存的密码哈希值。 */
    private String passwordHash;

    /** 用户创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
