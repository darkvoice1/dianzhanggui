package com.darkvoice1.dianzhanggui.common.validation;

/** 表示单个请求字段的校验失败原因。 */
public record ValidationError(String field, String message) {
}
