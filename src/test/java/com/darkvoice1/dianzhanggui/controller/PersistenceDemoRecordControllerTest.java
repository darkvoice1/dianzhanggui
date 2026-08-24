package com.darkvoice1.dianzhanggui.controller;

import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.entity.PersistenceDemoRecord;
import com.darkvoice1.dianzhanggui.exception.BusinessException;
import com.darkvoice1.dianzhanggui.service.PersistenceDemoRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证示例记录接口的参数校验和异常响应。 */
@WebMvcTest(PersistenceDemoRecordController.class)
class PersistenceDemoRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersistenceDemoRecordService recordService;

    /** 验证主键不是正整数时返回字段和校验原因。 */
    @Test
    void shouldReturnValidationErrorWhenIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/demo-records/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("请求参数不合法"))
                .andExpect(jsonPath("$.data[0].field").value("findById.id"))
                .andExpect(jsonPath("$.data[0].message").value("id 必须是正整数"));
    }

    /** 验证不存在的记录返回统一资源不存在响应。 */
    @Test
    void shouldReturnNotFoundWhenRecordDoesNotExist() throws Exception {
        given(recordService.findById(99L)).willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/demo-records/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("请求的资源不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /** 验证未知异常不会将内部错误信息暴露给调用方。 */
    @Test
    void shouldHideUnexpectedExceptionDetails() throws Exception {
        given(recordService.findById(500L)).willThrow(new IllegalStateException("内部数据库连接细节"));

        mockMvc.perform(get("/api/demo-records/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("内部数据库连接细节")));
    }
}
