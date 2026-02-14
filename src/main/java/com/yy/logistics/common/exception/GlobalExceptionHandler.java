package com.yy.logistics.common.exception;

import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case UNAUTHORIZED, BAD_CREDENTIALS, TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, ACCOUNT_DISABLED, ROLE_NOT_MATCH -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_FOUND, ORDER_NOT_FOUND, WAYBILL_NOT_FOUND, TASK_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BAD_REQUEST, VALIDATION_ERROR, ORDER_STATUS_INVALID, TASK_STATUS_INVALID -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return build(status, ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("数据约束异常: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ApiResponse.fail(ErrorCode.BAD_REQUEST, "数据冲突或字段不合法"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        String message = joinMessages(details, ErrorCode.VALIDATION_ERROR.getMessage());
        return build(HttpStatus.BAD_REQUEST, ApiResponse.fail(ErrorCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        String message = joinMessages(details, ErrorCode.VALIDATION_ERROR.getMessage());
        return build(HttpStatus.BAD_REQUEST, ApiResponse.fail(ErrorCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        List<String> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .toList();
        String message = joinMessages(details, ErrorCode.VALIDATION_ERROR.getMessage());
        return build(HttpStatus.BAD_REQUEST, ApiResponse.fail(ErrorCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(it -> it.getPropertyPath() + " " + it.getMessage())
                .toList();
        String message = joinMessages(details, ErrorCode.VALIDATION_ERROR.getMessage());
        return build(HttpStatus.BAD_REQUEST, ApiResponse.fail(ErrorCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        String message = "缺少必填参数: " + ex.getParameterName();
        return build(HttpStatus.BAD_REQUEST, ApiResponse.fail(ErrorCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = "参数类型错误: " + ex.getName();
        return build(HttpStatus.BAD_REQUEST, ApiResponse.fail(ErrorCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, ApiResponse.fail(ErrorCode.BAD_REQUEST, "请求体格式错误或字段类型不匹配"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ApiResponse.fail(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ApiResponse.fail(ErrorCode.NOT_FOUND, "请求路径不存在: " + ex.getResourcePath()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status == HttpStatus.NOT_FOUND) {
            return build(status, ApiResponse.fail(ErrorCode.NOT_FOUND));
        }
        if (status == HttpStatus.FORBIDDEN) {
            return build(status, ApiResponse.fail(ErrorCode.FORBIDDEN));
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            return build(status, ApiResponse.fail(ErrorCode.UNAUTHORIZED));
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return build(status, ApiResponse.fail(ErrorCode.BAD_REQUEST));
        }
        return build(status, ApiResponse.fail(ErrorCode.SYSTEM_ERROR, ex.getReason() == null ? ErrorCode.SYSTEM_ERROR.getMessage() : ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ApiResponse.fail(ErrorCode.SYSTEM_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, ApiResponse<Void> body) {
        return ResponseEntity.status(status).body(body);
    }

    private String formatFieldError(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        if (!StringUtils.hasText(defaultMessage)) {
            defaultMessage = "字段校验失败";
        }
        return error.getField() + ": " + defaultMessage;
    }

    private String joinMessages(List<String> messages, String fallback) {
        String merged = messages.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("; "));
        return StringUtils.hasText(merged) ? merged : fallback;
    }
}
