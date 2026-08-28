package com.darkvoice1.dianzhanggui.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.permission.mapper.PermissionMapper;
import com.darkvoice1.dianzhanggui.permission.mapper.RolePermissionMapper;
import com.darkvoice1.dianzhanggui.permission.model.Permission;
import com.darkvoice1.dianzhanggui.permission.model.RolePermission;
import com.darkvoice1.dianzhanggui.role.mapper.RoleMapper;
import com.darkvoice1.dianzhanggui.role.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证角色、权限及默认关联关系已正确初始化。 */
@SpringBootTest
@Testcontainers
class RolePermissionIntegrationTest {

    /** 启动角色权限集成测试使用的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_role_permission_test")
            .withUsername("test")
            .withPassword("test");

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

    /** 验证通用角色、权限和角色权限关系可以持久化。 */
    @Test
    void shouldPersistGenericRolePermissionDefinitions() {
        assertTrue(roleMapper.selectList(null).isEmpty());
        assertTrue(permissionMapper.selectList(null).isEmpty());

        Role owner = new Role();
        owner.setCode("OWNER");
        owner.setName("商家负责人");
        owner.setDescription("负责商家基础管理");
        roleMapper.insert(owner);

        Permission merchantManage = new Permission();
        merchantManage.setCode("MERCHANT_MANAGE");
        merchantManage.setName("商家管理");
        merchantManage.setDescription("管理商家基础信息");
        permissionMapper.insert(merchantManage);

        RolePermission relation = new RolePermission();
        relation.setRoleId(owner.getId());
        relation.setPermissionId(merchantManage.getId());
        rolePermissionMapper.insert(relation);

        Role savedRole = findRole("OWNER");
        Permission savedPermission = findPermission("MERCHANT_MANAGE");
        assertEquals(1, permissionsOf(savedRole).size());
        assertEquals(merchantManage.getId(), savedPermission.getId());
        assertEquals(owner.getId(), savedRole.getId());
    }

    /** 按角色编码读取默认角色。 */
    private Role findRole(String code) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        assertNotNull(role);
        return role;
    }

    /** 按权限编码读取默认权限。 */
    private Permission findPermission(String code) {
        Permission permission = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>().eq(Permission::getCode, code));
        assertNotNull(permission);
        return permission;
    }

    /** 查询指定角色拥有的权限关联记录。 */
    private List<RolePermission> permissionsOf(Role role) {
        return rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()));
    }
}
