package com.darkvoice1.dianzhanggui.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.customer.mapper.CustomerProfileMapper;
import com.darkvoice1.dianzhanggui.staff.mapper.StaffProfileMapper;
import com.darkvoice1.dianzhanggui.staff.model.StaffProfile;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证客户和人员档案的接口、权限、状态及租户隔离。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProfileIntegrationTest {

    /** 启动档案集成测试使用的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_profile_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private MerchantMemberMapper merchantMemberMapper;

    @Autowired
    private CustomerProfileMapper customerProfileMapper;

    @Autowired
    private StaffProfileMapper staffProfileMapper;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证客户档案可以创建、编辑和停用，顾客不能执行管理操作。 */
    @Test
    void shouldManageCustomerProfileAndRejectMemberWrite() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner-" + UUID.randomUUID() + "@example.com");
        String memberEmail = "member-" + UUID.randomUUID() + "@example.com";
        String memberToken = registerAndGetAccessToken(memberEmail);
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long memberUserId = findUserId(memberEmail);
        joinMerchant(memberToken, merchantId);

        Long profileId = createCustomerProfileAndGetId(ownerToken, merchantId, memberUserId);

        mockMvc.perform(patch("/api/customer-profiles/{id}", profileId)
                        .header("Authorization", "Bearer " + memberToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新客户\",\"phone\":\"13900000000\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(patch("/api/customer-profiles/{id}", profileId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新客户\",\"phone\":\"13900000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("新客户"));

        mockMvc.perform(delete("/api/customer-profiles/{id}", profileId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customer-profiles/{id}", profileId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/customer-profiles/{id}", profileId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"再次编辑\",\"phone\":\"13900000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROFILE_INACTIVE"));
    }

    /** 验证员工档案可以由有权限的商家成员编辑和停用。 */
    @Test
    void shouldManageStaffProfile() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner-" + UUID.randomUUID() + "@example.com");
        String employeeEmail = "employee-" + UUID.randomUUID() + "@example.com";
        String employeeToken = registerAndGetAccessToken(employeeEmail);
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long employeeUserId = findUserId(employeeEmail);
        joinMerchant(employeeToken, merchantId);
        changeMemberRole(ownerToken, merchantId, employeeUserId, "EMPLOYEE");

        StaffProfile staff = findStaffProfile(merchantId, employeeUserId);
        assertNotNull(staff);

        mockMvc.perform(patch("/api/staff-profiles/{id}", staff.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"前台员工\",\"phone\":\"13600000000\",\"position\":\"GENERAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("前台员工"));

        mockMvc.perform(delete("/api/staff-profiles/{id}", staff.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk());

        assertEquals("INACTIVE", staffProfileMapper.selectById(staff.getId()).getStatus());
    }

    /** 验证成员角色变化会在同一事务内同步两类档案状态。 */
    @Test
    void shouldSynchronizeProfilesWhenMemberRoleChanges() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner-" + UUID.randomUUID() + "@example.com");
        String memberEmail = "member-" + UUID.randomUUID() + "@example.com";
        String memberToken = registerAndGetAccessToken(memberEmail);
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long memberUserId = findUserId(memberEmail);
        joinMerchant(memberToken, merchantId);
        Long customerId = createCustomerProfileAndGetId(ownerToken, merchantId, memberUserId);

        changeMemberRole(ownerToken, merchantId, memberUserId, "EMPLOYEE");
        assertEquals("EMPLOYEE", findMember(merchantId, memberUserId).getRole());
        assertEquals("INACTIVE", customerProfileMapper.selectById(customerId).getStatus());
        assertEquals("ACTIVE", findStaffProfile(merchantId, memberUserId).getStatus());

        changeMemberRole(ownerToken, merchantId, memberUserId, "MEMBER");
        assertEquals("MEMBER", findMember(merchantId, memberUserId).getRole());
        assertEquals("ACTIVE", customerProfileMapper.selectById(customerId).getStatus());
        assertEquals("INACTIVE", findStaffProfile(merchantId, memberUserId).getStatus());
    }

    /** 验证同一档案 ID 在其他商家上下文中不可见。 */
    @Test
    void shouldNotAccessProfileFromAnotherMerchant() throws Exception {
        String firstOwnerToken = registerAndGetAccessToken("owner-" + UUID.randomUUID() + "@example.com");
        String secondOwnerToken = registerAndGetAccessToken("owner-" + UUID.randomUUID() + "@example.com");
        Long firstMerchantId = createMerchantAndGetId(firstOwnerToken);
        Long secondMerchantId = createMerchantAndGetId(secondOwnerToken);
        Long profileId = createCustomerProfileAndGetId(firstOwnerToken, firstMerchantId, null);

        mockMvc.perform(get("/api/customer-profiles/{id}", profileId)
                        .header("Authorization", "Bearer " + secondOwnerToken)
                        .header("X-Merchant-Id", secondMerchantId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /** 验证客户档案分页结果只包含当前商家的数据。 */
    @Test
    void shouldPageCustomerProfilesWithinCurrentMerchant() throws Exception {
        String ownerToken = registerAndGetAccessToken("profile-page-owner-" + UUID.randomUUID() + "@example.com");
        String otherOwnerToken = registerAndGetAccessToken("profile-page-other-" + UUID.randomUUID() + "@example.com");
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long otherMerchantId = createMerchantAndGetId(otherOwnerToken);
        createCustomerProfileAndGetId(ownerToken, merchantId, null);
        createCustomerProfileAndGetId(ownerToken, merchantId, null);
        createCustomerProfileAndGetId(otherOwnerToken, otherMerchantId, null);

        mockMvc.perform(get("/api/customer-profiles")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    /** 验证员工档案分页和商家成员角色筛选。 */
    @Test
    void shouldPageStaffProfilesAndMerchantMembers() throws Exception {
        String ownerToken = registerAndGetAccessToken("staff-page-owner-" + UUID.randomUUID() + "@example.com");
        String employeeEmail = "staff-page-employee-" + UUID.randomUUID() + "@example.com";
        String employeeToken = registerAndGetAccessToken(employeeEmail);
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long employeeUserId = findUserId(employeeEmail);
        joinMerchant(employeeToken, merchantId);
        changeMemberRole(ownerToken, merchantId, employeeUserId, "EMPLOYEE");
        StaffProfile staff = findStaffProfile(merchantId, employeeUserId);
        assertNotNull(staff);

        mockMvc.perform(get("/api/staff-profiles")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .param("page", "1")
                        .param("size", "1")
                        .param("keyword", staff.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/members", merchantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .param("page", "1")
                        .param("size", "10")
                        .param("role", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].role").value("EMPLOYEE"));
    }

    /** 创建测试商家并读取商家主键。 */
    private Long createMerchantAndGetId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantName\":\"档案测试商家\",\"firstStoreName\":\"中心店\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("merchantId").asLong();
    }

    /** 创建客户档案并读取档案主键。 */
    private Long createCustomerProfileAndGetId(String accessToken, Long merchantId, Long userId) throws Exception {
        String userIdJson = userId == null ? "null" : userId.toString();
        MvcResult result = mockMvc.perform(post("/api/customer-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试客户\",\"phone\":\"13800000000\",\"userId\":"
                                + userIdJson + "}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asLong();
    }

    /** 将用户加入商家，加入后默认是顾客角色。 */
    private void joinMerchant(String accessToken, Long merchantId) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/members", merchantId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /** 变更商家成员角色。 */
    private void changeMemberRole(String ownerToken, Long merchantId, Long userId, String role) throws Exception {
        mockMvc.perform(patch("/api/merchants/{merchantId}/members/{memberUserId}/role", merchantId, userId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk());
    }

    /** 注册测试用户并取得访问令牌。 */
    private String registerAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    /** 按邮箱查询测试用户主键。 */
    private Long findUserId(String email) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, email));
        assertNotNull(user);
        return user.getId();
    }

    /** 查询指定商家的成员关系。 */
    private MerchantMember findMember(Long merchantId, Long userId) {
        return merchantMemberMapper.selectOne(new LambdaQueryWrapper<MerchantMember>()
                .eq(MerchantMember::getMerchantId, merchantId)
                .eq(MerchantMember::getUserId, userId));
    }

    /** 查询指定商家的人员档案。 */
    private StaffProfile findStaffProfile(Long merchantId, Long userId) {
        return staffProfileMapper.selectOne(new LambdaQueryWrapper<StaffProfile>()
                .eq(StaffProfile::getMerchantId, merchantId)
                .eq(StaffProfile::getUserId, userId));
    }
}
