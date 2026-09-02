package com.darkvoice1.dianzhanggui.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.availability.mapper.ProductAvailabilityMapper;
import com.darkvoice1.dianzhanggui.availability.model.ProductAvailability;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证通用商品可用性预约的创建和客户资格校验。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ReservationIntegrationTest {

    /** 启动预约集成测试使用的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_reservation_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private ProductAvailabilityMapper productAvailabilityMapper;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证有效客户可以预约未来开放的商品资源。 */
    @Test
    void shouldCreateReservationForActiveCustomer() throws Exception {
        String ownerToken = registerAndGetAccessToken("reservation-owner-" + UUID.randomUUID() + "@example.com");
        String customerEmail = "reservation-customer-" + UUID.randomUUID() + "@example.com";
        String customerToken = registerAndGetAccessToken(customerEmail);
        Long merchantId = createMerchantAndGetId(ownerToken);
        Long customerUserId = findUserId(customerEmail);
        joinMerchant(customerToken, merchantId);
        createCustomerProfile(ownerToken, merchantId, customerUserId);
        Long productId = createAndPublishProduct(ownerToken, merchantId);
        ProductAvailability availability = createAvailability(merchantId, productId);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productAvailabilityId\":" + availability.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("RESERVED"))
                .andExpect(jsonPath("$.data.merchantId").value(merchantId));
    }

    /** 验证没有有效客户档案的登录用户不能创建预约。 */
    @Test
    void shouldRejectReservationWithoutActiveCustomerProfile() throws Exception {
        String ownerToken = registerAndGetAccessToken("reservation-owner-" + UUID.randomUUID() + "@example.com");
        String customerEmail = "reservation-no-profile-" + UUID.randomUUID() + "@example.com";
        String customerToken = registerAndGetAccessToken(customerEmail);
        Long merchantId = createMerchantAndGetId(ownerToken);
        joinMerchant(customerToken, merchantId);
        Long productId = createAndPublishProduct(ownerToken, merchantId);
        ProductAvailability availability = createAvailability(merchantId, productId);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productAvailabilityId\":" + availability.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESERVATION_CUSTOMER_REQUIRED"));
    }

    /** 创建测试商家并读取商家主键。 */
    private Long createMerchantAndGetId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantName\":\"预约测试商家\",\"firstStoreName\":\"中心店\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("merchantId").asLong();
    }

    /** 将用户加入商家。 */
    private void joinMerchant(String accessToken, Long merchantId) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/members", merchantId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /** 创建关联用户的客户档案。 */
    private void createCustomerProfile(String accessToken, Long merchantId, Long userId) throws Exception {
        mockMvc.perform(post("/api/customer-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"预约客户\",\"phone\":\"13800000000\",\"userId\":" + userId + "}"))
                .andExpect(status().isOk());
    }

    /** 创建并上架一个可预售或预约的商品。 */
    private Long createAndPublishProduct(String accessToken, Long merchantId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product-services")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"预约商品\",\"type\":\"PRODUCT\","
                                + "\"originalPrice\":100.00,\"sellingPrice\":80.00}"))
                .andExpect(status().isOk())
                .andReturn();
        Long productId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(post("/api/product-services/{id}/publish", productId)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Merchant-Id", merchantId))
                .andExpect(status().isOk());
        return productId;
    }

    /** 直接准备测试用的未来商品可用性记录。 */
    private ProductAvailability createAvailability(Long merchantId, Long productId) {
        ProductAvailability availability = new ProductAvailability();
        availability.setMerchantId(merchantId);
        availability.setProductId(productId);
        availability.setStartAt(LocalDateTime.now().plusDays(1));
        availability.setEndAt(LocalDateTime.now().plusDays(1).plusHours(1));
        availability.setCapacity(10);
        availability.setRemainingCapacity(10);
        availability.setStatus("OPEN");
        productAvailabilityMapper.insert(availability);
        return availability;
    }

    /** 注册测试用户并取得访问令牌。 */
    private String registerAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("accessToken").asText();
    }

    /** 按邮箱查询测试用户主键。 */
    private Long findUserId(String email) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, email));
        return user.getId();
    }
}
