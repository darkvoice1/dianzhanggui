package com.darkvoice1.dianzhanggui.permission.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 表示一个角色与一个权限之间的关联关系。 */
@TableName("role_permission")
public class RolePermission {

    /** 角色权限关联主键。 */
    @TableId
    private Long id;

    /** 角色主键。 */
    private Long roleId;

    /** 权限主键。 */
    private Long permissionId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
}
