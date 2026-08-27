package com.darkvoice1.dianzhanggui.common;

/** 定义当前阶段通用的接口响应错误码。 */
public enum ErrorCode {

    /** 请求处理成功。 */
    SUCCESS("SUCCESS", "操作成功"),

    /** 请求参数不符合校验规则。 */
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数不合法"),

    /** 请求的数据类型无法转换。 */
    PARAMETER_TYPE_ERROR("PARAMETER_TYPE_ERROR", "请求参数类型不正确"),

    /** 请求的资源不存在。 */
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "请求的资源不存在"),

    /** 业务规则不允许当前操作。 */
    BUSINESS_ERROR("BUSINESS_ERROR", "业务处理失败"),

    /** 用户登录凭证不正确。 */
    LOGIN_FAILED("LOGIN_FAILED", "邮箱或密码不正确"),

    /** 邮箱已被其他用户注册。 */
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "该邮箱已注册"),

    /** 用户已是该商家的成员。 */
    MERCHANT_MEMBER_ALREADY_EXISTS("MERCHANT_MEMBER_ALREADY_EXISTS", "您已加入该商家"),

    /** 用户无权使用指定商家。 */
    MERCHANT_ACCESS_DENIED("MERCHANT_ACCESS_DENIED", "您尚未加入该商家"),

    /** 租户业务请求缺少当前商家。 */
    TENANT_REQUIRED("TENANT_REQUIRED", "请先选择当前商家"),

    /** 请求缺少有效登录凭证。 */
    UNAUTHORIZED("UNAUTHORIZED", "请先登录"),

    /** 未预期的系统异常。 */
    INTERNAL_ERROR("INTERNAL_ERROR", "系统繁忙，请稍后重试");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 获取返回给调用方的错误码。 */
    public String code() {
        return code;
    }

    /** 获取返回给调用方的默认错误信息。 */
    public String message() {
        return message;
    }
}
