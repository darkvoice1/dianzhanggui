package com.darkvoice1.dianzhanggui.catalog;

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

/** 验证商品与服务目录的权限、价格、状态和租户隔离。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CatalogIntegrationTest {

    /** 启动目录集成测试使用的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_catalog_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证老板可以创建、上架和下架商品，重复上架会被拒绝。 */
    @Test
    void shouldManageCatalogAndValidateStatus() throws Exception {
        String ownerToken = registerAndGetAccessToken("catalog-owner-" + UUID.randomUUID() + "@example.com");
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long productId = createProductAndGetId(ownerToken, merchantId, "PRODUCT", "100.00", "80.00");

        mockMvc.perform(post("/api/product-services/{id}/unpublish", productId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));

        mockMvc.perform(post("/api/product-services/{id}/publish", productId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));

        mockMvc.perform(post("/api/product-services/{id}/publish", productId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));

        mockMvc.perform(post("/api/product-services/{id}/unpublish", productId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFF_SALE"));
    }

    /** 验证老板可以编辑商品基础资料和价格。 */
    @Test
    void shouldUpdateProductDetailsAndPrices() throws Exception {
        String ownerToken = registerAndGetAccessToken("catalog-update-owner-" + UUID.randomUUID() + "@example.com");
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long productId = createProductAndGetId(ownerToken, merchantId, "PRODUCT", "100.00", "80.00");

        mockMvc.perform(patch("/api/product-services/{id}", productId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("新版服务", "SERVICE", "300.00", "260.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("新版服务"))
                .andExpect(jsonPath("$.data.type").value("SERVICE"))
                .andExpect(jsonPath("$.data.originalPrice").value(300.00))
                .andExpect(jsonPath("$.data.sellingPrice").value(260.00));
    }

    /** 验证商品目录分页筛选、分页上限和跨商家列表隔离。 */
    @Test
    void shouldPageAndFilterCatalogWithinCurrentMerchant() throws Exception {
        String ownerToken = registerAndGetAccessToken("catalog-page-owner-" + UUID.randomUUID() + "@example.com");
        String otherOwnerToken = registerAndGetAccessToken("catalog-page-other-" + UUID.randomUUID() + "@example.com");
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long otherMerchantId = createMerchantAndGetId(otherOwnerToken);
        createProductAndGetId(ownerToken, merchantId, "PRODUCT", "100.00", "80.00");
        createProductAndGetId(ownerToken, merchantId, "SERVICE", "200.00", "150.00");

        mockMvc.perform(get("/api/product-services")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .param("page", "1")
                        .param("size", "1")
                        .param("type", "SERVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1));

        mockMvc.perform(get("/api/product-services")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/product-services")
                        .header("Authorization", "Bearer " + otherOwnerToken)
                        .header("X-Merchant-Id", otherMerchantId)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    /** 验证顾客没有目录管理权限，非法价格无法创建。 */
    @Test
    void shouldRejectMemberAndInvalidPrice() throws Exception {
        String ownerToken = registerAndGetAccessToken("catalog-owner-" + UUID.randomUUID() + "@example.com");
        String memberToken = registerAndGetAccessToken("catalog-member-" + UUID.randomUUID() + "@example.com");
        Long merchantId = createMerchantAndGetId(ownerToken);
        joinMerchant(memberToken, merchantId);

        mockMvc.perform(post("/api/product-services")
                        .header("Authorization", "Bearer " + memberToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("商品", "PRODUCT", "100.00", "80.00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(post("/api/product-services")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("错误价格", "SERVICE", "100.00", "120.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    /** 验证其他商家不能读取当前商家的目录记录。 */
    @Test
    void shouldRejectCatalogFromAnotherMerchant() throws Exception {
        String firstOwnerToken = registerAndGetAccessToken("catalog-owner-" + UUID.randomUUID() + "@example.com");
        String secondOwnerToken = registerAndGetAccessToken("catalog-owner-" + UUID.randomUUID() + "@example.com");
        Long firstMerchantId = createMerchantAndGetId(firstOwnerToken);
        Long secondMerchantId = createMerchantAndGetId(secondOwnerToken);
        Long productId = createProductAndGetId(firstOwnerToken, firstMerchantId, "SERVICE", "50.00", "50.00");

        mockMvc.perform(get("/api/product-services/{id}", productId)
                        .header("Authorization", "Bearer " + secondOwnerToken)
                        .header("X-Merchant-Id", secondMerchantId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /** 创建测试商家并读取商家主键。 */
    private Long createMerchantAndGetId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantName\":\"目录测试商家\",\"firstStoreName\":\"中心店\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("merchantId").asLong();
    }

    /** 创建商品或服务并读取目录主键。 */
    private Long createProductAndGetId(String accessToken, Long merchantId, String type,
            String originalPrice, String sellingPrice) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product-services")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("测试目录", type, originalPrice, sellingPrice)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asLong();
    }

    /** 将当前用户以顾客身份加入商家。 */
    private void joinMerchant(String accessToken, Long merchantId) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/members", merchantId)
                        .header("Authorization", "Bearer " + accessToken))
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

    /** 生成商品或服务创建请求 JSON。 */
    private String productJson(String name, String type, String originalPrice, String sellingPrice) {
        return "{\"name\":\"" + name + "\",\"type\":\"" + type
                + "\",\"description\":\"测试目录\",\"originalPrice\":" + originalPrice
                + ",\"sellingPrice\":" + sellingPrice + "}";
    }
}
