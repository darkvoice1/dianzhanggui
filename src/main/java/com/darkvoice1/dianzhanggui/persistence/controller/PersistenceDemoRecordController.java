package com.darkvoice1.dianzhanggui.persistence.controller;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.persistence.model.CreatePersistenceDemoRecordRequest;
import com.darkvoice1.dianzhanggui.persistence.model.PersistenceDemoRecord;
import com.darkvoice1.dianzhanggui.persistence.service.PersistenceDemoRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供示例记录的创建和查询接口，用于验证持久层闭环。 */
@RestController
@RequestMapping("/api/demo-records")
@Validated
public class PersistenceDemoRecordController {

    private final PersistenceDemoRecordService recordService;

    /** 创建示例记录控制器并注入业务服务。 */
    public PersistenceDemoRecordController(PersistenceDemoRecordService recordService) {
        this.recordService = recordService;
    }

    /** 在当前商家范围内创建示例记录。 */
    @PostMapping
    public ApiResponse<PersistenceDemoRecord> create(@Valid @RequestBody CreatePersistenceDemoRecordRequest request) {
        return ApiResponse.success(recordService.create(request.name().trim()));
    }

    /** 根据主键查询示例记录。 */
    @GetMapping("/{id}")
    public ApiResponse<PersistenceDemoRecord> findById(@PathVariable @Positive(message = "id 必须是正整数") Long id) {
        return ApiResponse.success(recordService.findById(id));
    }
}
