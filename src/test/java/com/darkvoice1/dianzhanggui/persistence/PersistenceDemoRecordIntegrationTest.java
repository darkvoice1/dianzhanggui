package com.darkvoice1.dianzhanggui.persistence;

import com.darkvoice1.dianzhanggui.persistence.mapper.PersistenceDemoRecordMapper;
import com.darkvoice1.dianzhanggui.persistence.model.PersistenceDemoRecord;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private PersistenceDemoRecordMapper recordMapper;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证数据库插入、查询和 HTTP 查询接口。 */
    @Test
    void shouldInsertAndQueryRecord() throws Exception {
        String accessToken = registerAndGetAccessToken();
        Long merchantId = createMerchantAndGetId(accessToken);
        PersistenceDemoRecord record = new PersistenceDemoRecord();
        record.setName("持久层测试记录");
        record.setMerchantId(merchantId);
        recordMapper.insert(record);

        mockMvc.perform(get("/api/demo-records/{id}", record.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(record.getId()))
                .andExpect(jsonPath("$.data.name").value("持久层测试记录"));
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
