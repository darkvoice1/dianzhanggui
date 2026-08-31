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


/** 验证固定角色、权限及角色权限关系的数据模型。 */
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

    /** 验证系统初始化固定角色，并支持持久化权限关联。 */
    @Test
    void shouldInitializeFixedRolesAndPersistPermissionRelations() {
        assertEquals(3, roleMapper.selectList(null).size());
        assertEquals(2, permissionMapper.selectList(null).size());
        assertEquals(3, rolePermissionMapper.selectList(null).size());

        Role owner = findRole("OWNER");
        assertEquals("老板", owner.getName());
        assertEquals("员工", findRole("EMPLOYEE").getName());
        assertEquals("顾客", findRole("MEMBER").getName());
        assertEquals("MERCHANT_MEMBER_MANAGE", findPermission("MERCHANT_MEMBER_MANAGE").getCode());
        assertEquals(2, permissionsOf(owner).size());

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
        assertEquals(3, permissionsOf(savedRole).size());
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
