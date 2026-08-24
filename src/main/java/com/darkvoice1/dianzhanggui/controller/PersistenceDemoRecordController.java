package com.darkvoice1.dianzhanggui.controller;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.entity.PersistenceDemoRecord;
import com.darkvoice1.dianzhanggui.service.PersistenceDemoRecordService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供示例记录的查询接口，用于验证持久层闭环。 */
@RestController
@RequestMapping("/api/demo-records")
@Validated
public class PersistenceDemoRecordController {

    private final PersistenceDemoRecordService recordService;

    /** 创建示例记录控制器并注入业务服务。 */
    public PersistenceDemoRecordController(PersistenceDemoRecordService recordService) {
        this.recordService = recordService;
    }

    /** 根据主键查询示例记录。 */
    @GetMapping("/{id}")
    public ApiResponse<PersistenceDemoRecord> findById(@PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(recordService.findById(id));
    }
}
