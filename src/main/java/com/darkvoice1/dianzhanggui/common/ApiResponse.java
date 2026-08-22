package com.darkvoice1.dianzhanggui.common;

/** 定义项目统一的接口响应结构。 */
public record ApiResponse<T>(String code, String message, T data) {

    /** 创建一个表示操作成功的响应。 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "操作成功", data);
    }
}
