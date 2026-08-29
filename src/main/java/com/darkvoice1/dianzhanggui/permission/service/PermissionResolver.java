package com.darkvoice1.dianzhanggui.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.permission.mapper.PermissionMapper;
import com.darkvoice1.dianzhanggui.permission.mapper.RolePermissionMapper;
import com.darkvoice1.dianzhanggui.permission.model.Permission;
import com.darkvoice1.dianzhanggui.permission.model.RolePermission;
import com.darkvoice1.dianzhanggui.role.mapper.RoleMapper;
import com.darkvoice1.dianzhanggui.role.model.Role;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 根据用户在当前商家的角色解析权限编码。 */
@Service
public class PermissionResolver {

    private final MerchantMemberMapper merchantMemberMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    /** 创建权限解析服务并注入角色权限查询组件。 */
    public PermissionResolver(MerchantMemberMapper merchantMemberMapper, RoleMapper roleMapper,
                              RolePermissionMapper rolePermissionMapper, PermissionMapper permissionMapper) {
        this.merchantMemberMapper = merchantMemberMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    /** 根据用户和指定商家解析权限，商家成员关系不存在时拒绝访问。 */
    public Set<String> resolvePermissions(Long userId, Long merchantId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (merchantId == null) {
            throw new BusinessException(ErrorCode.TENANT_REQUIRED);
        }

        MerchantMember member = merchantMemberMapper.selectOne(new LambdaQueryWrapper<MerchantMember>()
                .eq(MerchantMember::getUserId, userId)
                .eq(MerchantMember::getMerchantId, merchantId));
        if (member == null) {
            throw new BusinessException(ErrorCode.MERCHANT_ACCESS_DENIED);
        }

        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, member.getRole()));
        if (role == null) {
            return Collections.emptySet();
        }

        Set<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()))
                .stream()
                .map(RolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (permissionIds.isEmpty()) {
            return Collections.emptySet();
        }

        return permissionMapper.selectByIds(permissionIds).stream()
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 根据当前租户上下文解析用户权限。 */
    public Set<String> resolveCurrentPermissions(Long userId) {
        return resolvePermissions(userId, TenantContext.requireMerchantId());
    }

    /** 判断用户在当前租户中是否拥有指定权限。 */
    public boolean hasPermission(Long userId, String permissionCode) {
        return permissionCode != null && resolveCurrentPermissions(userId).contains(permissionCode);
    }

    /** 要求用户在当前租户中拥有指定权限，缺失时拒绝操作。 */
    public void requirePermission(Long userId, String permissionCode) {
        if (!hasPermission(userId, permissionCode)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
    }
}
