package com.darkvoice1.dianzhanggui.exception;

import com.darkvoice1.dianzhanggui.common.ErrorCode;

/** 表示可以预期并安全返回给调用方的业务异常。 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 使用错误码默认提示创建业务异常。 */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /** 使用指定提示创建业务异常。 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 获取业务异常对应的错误码。 */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
