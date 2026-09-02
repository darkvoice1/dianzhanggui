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

    /** 用户没有执行当前操作所需的权限。 */
    PERMISSION_DENIED("PERMISSION_DENIED", "您没有执行当前操作的权限"),

    /** 商家老板角色不允许通过成员管理接口变更。 */
    OWNER_ROLE_CHANGE_NOT_ALLOWED("OWNER_ROLE_CHANGE_NOT_ALLOWED", "商家老板角色不能变更"),

    /** 当前商家已经存在该客户档案。 */
    CUSTOMER_PROFILE_ALREADY_EXISTS("CUSTOMER_PROFILE_ALREADY_EXISTS", "该客户档案已存在"),

    /** 当前商家已经存在该人员档案。 */
    STAFF_PROFILE_ALREADY_EXISTS("STAFF_PROFILE_ALREADY_EXISTS", "该人员档案已存在"),

    /** 当前档案已停用，不能继续执行需要有效档案的操作。 */
    PROFILE_INACTIVE("PROFILE_INACTIVE", "档案已停用"),

    /** 当前档案已经是停用状态。 */
    PROFILE_ALREADY_INACTIVE("PROFILE_ALREADY_INACTIVE", "档案已经停用"),

    /** 当前用户在商家下没有可用的客户档案。 */
    RESERVATION_CUSTOMER_REQUIRED("RESERVATION_CUSTOMER_REQUIRED", "请先完善有效的客户档案"),

    /** 请求的商品可用性记录不存在。 */
    PRODUCT_AVAILABILITY_NOT_FOUND("PRODUCT_AVAILABILITY_NOT_FOUND", "商品可用性记录不存在"),

    /** 商品可用性当前不满足预约条件。 */
    PRODUCT_AVAILABILITY_NOT_BOOKABLE("PRODUCT_AVAILABILITY_NOT_BOOKABLE", "当前商品暂不可预约"),

    /** 请求的预约记录不存在或不属于当前用户。 */
    RESERVATION_NOT_FOUND("RESERVATION_NOT_FOUND", "预约记录不存在"),

    /** 当前预约不满足取消条件。 */
    RESERVATION_CANCELLATION_NOT_ALLOWED("RESERVATION_CANCELLATION_NOT_ALLOWED", "当前预约不允许取消"),

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
