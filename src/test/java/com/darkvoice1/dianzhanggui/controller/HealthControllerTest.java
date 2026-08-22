package com.darkvoice1.dianzhanggui.controller;

import com.darkvoice1.dianzhanggui.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 健康检查控制器的接口测试。 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthService healthService;

    /** 验证健康检查接口返回统一成功响应和 UP 状态。 */
    @Test
    void shouldReturnHealthyStatus() throws Exception {
        // 模拟服务层返回健康状态，隔离控制器测试范围。
        given(healthService.getStatus()).willReturn(new HealthService.HealthStatus("UP"));

        // 发起请求并校验 HTTP 状态码和 JSON 响应字段。
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
