package com.darkvoice1.dianzhanggui.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMapper;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.mapper.StoreMapper;
import com.darkvoice1.dianzhanggui.tenant.model.Merchant;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import com.darkvoice1.dianzhanggui.tenant.model.Store;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证商家、门店和成员关系的创建链路。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MerchantIntegrationTest {

    /** 启动商家集成测试使用的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_merchant_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private MerchantMemberMapper merchantMemberMapper;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证当前登录用户可以创建商家、首个门店和创建者成员关系。 */
    @Test
    void shouldCreateMerchantWithFirstStoreAndOwnerMembership() throws Exception {
        String email = "merchant-" + UUID.randomUUID() + "@example.com";
        String accessToken = registerAndGetAccessToken(email);

        mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantName":"一二健身","firstStoreName":"朝阳店","firstStoreAddress":"朝阳区"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.merchantName").value("一二健身"))
                .andExpect(jsonPath("$.data.firstStoreName").value("朝阳店"));

        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, email));
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getName, "一二健身"));
        assertNotNull(user);
        assertNotNull(merchant);

        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
                .eq(Store::getMerchantId, merchant.getId()));
        MerchantMember member = merchantMemberMapper.selectOne(new LambdaQueryWrapper<MerchantMember>()
                .eq(MerchantMember::getMerchantId, merchant.getId())
                .eq(MerchantMember::getUserId, user.getId()));
        assertNotNull(store);
        assertEquals("朝阳店", store.getName());
        assertEquals("朝阳区", store.getAddress());
        assertNotNull(member);
        assertEquals("OWNER", member.getRole());
    }

    /** 验证用户可加入商家、查询所属商家，并选择已加入的商家。 */
    @Test
    void shouldJoinListAndSwitchMerchant() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner-" + UUID.randomUUID() + "@example.com");
        String memberToken = registerAndGetAccessToken("member-" + UUID.randomUUID() + "@example.com");
        Long merchantId = createMerchantAndGetId(ownerToken);

        mockMvc.perform(post("/api/merchants/{merchantId}/members", merchantId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(get("/api/merchants/mine")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].merchantId").value(merchantId))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"));

        mockMvc.perform(post("/api/merchants/{merchantId}/switch", merchantId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantId").value(merchantId))
                .andExpect(jsonPath("$.data.role").value("MEMBER"));
    }

    /** 创建测试商家并读取接口返回的商家主键。 */
    private Long createMerchantAndGetId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantName":"星河健身","firstStoreName":"西城店"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("merchantId").asLong();
    }

    /** 注册测试用户并取得访问令牌。 */
    private String registerAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }
}
