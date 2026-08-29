package com.darkvoice1.dianzhanggui.persistence.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 表示修改示例业务记录时提交的信息。 */
public record UpdatePersistenceDemoRecordRequest(
        @NotBlank(message = "记录名称不能为空")
        @Size(max = 100, message = "记录名称不能超过 100 个字符")
        String name) {
}
