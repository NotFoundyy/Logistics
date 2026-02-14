package com.yy.logistics.common.api;

import com.yy.logistics.common.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一响应体")
public record ApiResponse<T>(
        @Schema(description = "业务码，00000 表示成功", example = "00000")
        String code,
        @Schema(description = "响应消息", example = "操作成功")
        String message,
        @Schema(description = "响应数据")
        T data,
        @Schema(description = "响应时间戳（毫秒）", example = "1739350800000")
        long timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), message, data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, System.currentTimeMillis());
    }
}