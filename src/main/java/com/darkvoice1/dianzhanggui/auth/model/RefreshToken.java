package com.darkvoice1.dianzhanggui.auth.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 表示保存在数据库中的刷新令牌记录。 */
@TableName("refresh_token")
public class RefreshToken {

    /** 刷新令牌记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户主键。 */
    private Long userId;

    /** 刷新令牌的 SHA-256 哈希值。 */
    private String tokenHash;

    /** 刷新令牌过期时间。 */
    private LocalDateTime expiresAt;

    /** 刷新令牌注销时间，空值表示仍可使用。 */
    private LocalDateTime revokedAt;

    /** 刷新令牌创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
