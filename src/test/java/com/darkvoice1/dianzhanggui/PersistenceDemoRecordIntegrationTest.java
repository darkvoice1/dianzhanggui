package com.darkvoice1.dianzhanggui;

import com.darkvoice1.dianzhanggui.entity.PersistenceDemoRecord;
import com.darkvoice1.dianzhanggui.mapper.PersistenceDemoRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        PersistenceDemoRecord record = new PersistenceDemoRecord();
        record.setName("持久层测试记录");
        recordMapper.insert(record);

        mockMvc.perform(get("/api/demo-records/{id}", record.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(record.getId()))
                .andExpect(jsonPath("$.data.name").value("持久层测试记录"));
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
