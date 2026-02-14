package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "取消订单请求")
public record OrderCancelRequest(
        @Schema(description = "取消原因", example = "收件信息填写错误")
        @NotBlank(message = "取消原因不能为空")
        @Size(max = 100, message = "取消原因长度不能超过100")
        String reason
) {
}
