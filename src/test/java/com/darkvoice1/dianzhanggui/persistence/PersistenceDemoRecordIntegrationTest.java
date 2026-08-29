package com.darkvoice1.dianzhanggui.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证 PostgreSQL、Flyway、MyBatis-Plus 和查询接口的完整链路。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PersistenceDemoRecordIntegrationTest {

    /** 启动用于集成测试的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证示例记录创建和查询都会使用当前商家。 */
    @Test
    void shouldCreateAndQueryRecordInCurrentMerchant() throws Exception {
        String accessToken = registerAndGetAccessToken();
        Long merchantId = createMerchantAndGetId(accessToken);
        Long recordId = createRecordAndGetId(accessToken, merchantId, "持久层测试记录");

        mockMvc.perform(get("/api/demo-records/{id}", recordId)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(recordId))
                .andExpect(jsonPath("$.data.merchantId").value(merchantId))
                .andExpect(jsonPath("$.data.name").value("持久层测试记录"));
    }

    /** 验证当前商家可以修改并删除自己创建的示例记录。 */
    @Test
    void shouldUpdateAndDeleteRecordInCurrentMerchant() throws Exception {
        String accessToken = registerAndGetAccessToken();
        Long merchantId = createMerchantAndGetId(accessToken);
        Long recordId = createRecordAndGetId(accessToken, merchantId, "原始名称");

        mockMvc.perform(patch("/api/demo-records/{id}", recordId)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"修改后名称\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(recordId))
                .andExpect(jsonPath("$.data.merchantId").value(merchantId))
                .andExpect(jsonPath("$.data.name").value("修改后名称"));

        mockMvc.perform(delete("/api/demo-records/{id}", recordId)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(get("/api/demo-records/{id}", recordId)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /** 验证租户业务请求未选择商家时会被拒绝。 */
    @Test
    void shouldRequireMerchantHeaderForDemoRecord() throws Exception {
        mockMvc.perform(get("/api/demo-records/1")
                        .header("Authorization", "Bearer " + registerAndGetAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"));
    }

    /** 验证用户不能伪造其他商家的请求头访问租户业务。 */
    @Test
    void shouldRejectAnotherMerchantsHeader() throws Exception {
        Long merchantId = createMerchantAndGetId(registerAndGetAccessToken());
        String otherUserToken = registerAndGetAccessToken();

        mockMvc.perform(get("/api/demo-records/1")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MERCHANT_ACCESS_DENIED"));
    }

    /** 创建测试商家并读取接口返回的商家主键。 */
    private Long createMerchantAndGetId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantName":"租户测试商家","firstStoreName":"测试门店"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("merchantId")
                .asLong();
    }

    /** 在指定当前商家中创建示例记录并读取生成的主键。 */
    private Long createRecordAndGetId(String accessToken, Long merchantId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/demo-records")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.merchantId").value(merchantId))
                .andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
    }

    /** 注册测试用户并取得访问受保护接口的 JWT。 */
    private String registerAndGetAccessToken() throws Exception {
        String email = "record-test-" + java.util.UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response)
                .path("data")
                .path("accessToken")
                .asText();
    }

    /** 验证应用提供包含项目基本信息的 OpenAPI 文档。 */
    @Test
    void shouldExposeOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("店掌柜 API"))
                .andExpect(jsonPath("$.paths['/api/health']").exists());
    }
}
