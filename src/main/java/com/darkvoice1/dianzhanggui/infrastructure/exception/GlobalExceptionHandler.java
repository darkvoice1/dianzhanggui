package com.darkvoice1.dianzhanggui.infrastructure.exception;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.validation.ValidationError;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/** 将参数错误、业务异常和未知异常转换为统一响应。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 返回请求体字段校验失败的字段和原因。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, errors));
    }

    /** 返回路径参数或查询参数校验失败的字段和原因。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleConstraintViolation(
            ConstraintViolationException exception) {
        List<ValidationError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, errors));
    }

    /** 返回无法转换的路径参数或查询参数信息。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        ValidationError error = new ValidationError(exception.getName(), "参数类型不正确");
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.PARAMETER_TYPE_ERROR, List.of(error)));
    }

    /** 返回可预期的业务异常，不输出错误堆栈。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = switch (errorCode) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case LOGIN_FAILED, UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case USER_ALREADY_EXISTS, MERCHANT_MEMBER_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case MERCHANT_ACCESS_DENIED, PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        log.warn("event=business_exception code={} message={}", errorCode.code(), exception.getMessage());
        return ResponseEntity.status(status).body(ApiResponse.failure(errorCode, null));
    }

    /** 记录未知异常详情，并避免将堆栈信息暴露给调用方。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("event=unexpected_exception type={} message={}",
                exception.getClass().getSimpleName(), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, null));
    }

    /** 将 Spring 字段校验错误转换为统一字段错误对象。 */
    private ValidationError toValidationError(FieldError error) {
        return new ValidationError(error.getField(), error.getDefaultMessage());
    }
}
