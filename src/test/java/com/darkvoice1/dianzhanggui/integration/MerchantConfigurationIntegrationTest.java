package com.darkvoice1.dianzhanggui.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证商家从初始化到基础经营资料配置的完整接口链路。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MerchantConfigurationIntegrationTest {

    /** 启动商家配置测试使用的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_configuration_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证注册用户可以完成成员、档案和商品目录的基础配置。 */
    @Test
    void shouldCompleteMerchantConfigurationFlow() throws Exception {
        String ownerToken = registerAndGetAccessToken("configuration-owner-" + UUID.randomUUID() + "@example.com");
        String customerEmail = "configuration-customer-" + UUID.randomUUID() + "@example.com";
        String customerToken = registerAndGetAccessToken(customerEmail);
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long customerUserId = findUserId(customerEmail);

        mockMvc.perform(post("/api/merchants/{merchantId}/members", merchantId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/product-services")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(post("/api/customer-profiles")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"配置顾客\",\"phone\":\"13800000000\",\"userId\":"
                                + customerUserId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/merchants/{merchantId}/members/{memberUserId}/role", merchantId, customerUserId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/staff-profiles")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .param("keyword", "配置顾客"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"));

        Long productId = createProductAndGetId(ownerToken, merchantId);
        mockMvc.perform(post("/api/product-services/{id}/publish", productId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));

        mockMvc.perform(get("/api/product-services")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .param("status", "ON_SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(productId));

    }

    /** 创建测试商家并读取商家主键。 */
    private Long createMerchantAndGetId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantName\":\"配置测试商家\",\"firstStoreName\":\"中心店\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("merchantId").asLong();
    }

    /** 创建商品并读取商品主键。 */
    private Long createProductAndGetId(String accessToken, Long merchantId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product-services")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asLong();
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
        return user.getId();
    }

    /** 生成商品创建请求 JSON。 */
    private String productJson() {
        return "{\"name\":\"配置商品\",\"type\":\"PRODUCT\",\"description\":\"基础商品\","
                + "\"originalPrice\":100.00,\"sellingPrice\":80.00}";
    }
}
