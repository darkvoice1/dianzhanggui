package com.darkvoice1.dianzhanggui.permission;

import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.permission.mapper.PermissionMapper;
import com.darkvoice1.dianzhanggui.permission.mapper.RolePermissionMapper;
import com.darkvoice1.dianzhanggui.permission.model.Permission;
import com.darkvoice1.dianzhanggui.permission.model.RolePermission;
import com.darkvoice1.dianzhanggui.permission.service.PermissionResolver;
import com.darkvoice1.dianzhanggui.role.mapper.RoleMapper;
import com.darkvoice1.dianzhanggui.role.model.Role;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMapper;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.model.Merchant;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证用户角色可以在商家租户内解析为权限集合。 */
@SpringBootTest
@Testcontainers
class PermissionResolverIntegrationTest {

    /** 启动权限解析集成测试使用的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_permission_resolver_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PermissionResolver permissionResolver;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private MerchantMemberMapper merchantMemberMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证成员角色可以解析为对应的权限编码集合。 */
    @Test
    void shouldResolvePermissionsForMerchantMember() {
        UserAccount user = createUser();
        Merchant merchant = createMerchant();
        createMembership(user, merchant, "OWNER");

        Role role = findRole("OWNER");
        Permission merchantManage = createPermission("MERCHANT_MANAGE");
        Permission orderView = createPermission("ORDER_VIEW");
        createRolePermission(role, merchantManage);
        createRolePermission(role, orderView);

        Set<String> permissions = permissionResolver.resolvePermissions(user.getId(), merchant.getId());

        assertEquals(Set.of("MERCHANT_MANAGE", "ORDER_VIEW"), permissions);
    }

    /** 验证用户不属于指定商家时不能解析该商家的权限。 */
    @Test
    void shouldRejectUserOutsideMerchant() {
        UserAccount user = createUser();
        Merchant merchant = createMerchant();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> permissionResolver.resolvePermissions(user.getId(), merchant.getId()));

        assertEquals("MERCHANT_ACCESS_DENIED", exception.getErrorCode().code());
    }

    /** 验证当前租户上下文可以驱动权限解析和权限判断。 */
    @Test
    void shouldResolvePermissionsFromCurrentTenantContext() {
        UserAccount user = createUser();
        Merchant merchant = createMerchant();
        createMembership(user, merchant, "EMPLOYEE");

        Role role = findRole("EMPLOYEE");
        Permission staffView = createPermission("STAFF_VIEW");
        createRolePermission(role, staffView);

        TenantContext.setMerchantId(merchant.getId());
        try {
            assertEquals(Set.of("STAFF_VIEW"), permissionResolver.resolveCurrentPermissions(user.getId()));
            assertTrue(permissionResolver.hasPermission(user.getId(), "STAFF_VIEW"));
            assertFalse(permissionResolver.hasPermission(user.getId(), "ORDER_VIEW"));
        } finally {
            TenantContext.clear();
        }
    }

    /** 创建权限解析测试使用的用户。 */
    private UserAccount createUser() {
        UserAccount user = new UserAccount();
        user.setEmail("permission-resolver-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("test-hash");
        userAccountMapper.insert(user);
        return user;
    }

    /** 创建权限解析测试使用的商家。 */
    private Merchant createMerchant() {
        Merchant merchant = new Merchant();
        merchant.setName("权限解析测试商家-" + UUID.randomUUID());
        merchantMapper.insert(merchant);
        return merchant;
    }

    /** 创建用户在指定商家中的成员关系。 */
    private void createMembership(UserAccount user, Merchant merchant, String roleCode) {
        MerchantMember member = new MerchantMember();
        member.setUserId(user.getId());
        member.setMerchantId(merchant.getId());
        member.setRole(roleCode);
        merchantMemberMapper.insert(member);
    }

    /** 按角色编码读取系统固定角色。 */
    private Role findRole(String code) {
        return roleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                .eq(Role::getCode, code));
    }

    /** 创建权限定义。 */
    private Permission createPermission(String code) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(code);
        permissionMapper.insert(permission);
        return permission;
    }

    /** 创建角色与权限的关联关系。 */
    private void createRolePermission(Role role, Permission permission) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionId(permission.getId());
        rolePermissionMapper.insert(rolePermission);
    }
}
