package com.yy.logistics.workbench.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "快递员主动取消订单请求")
public record CourierCancelOrderRequest(
        @Schema(description = "取消原因", example = "收件地址无法到达，已通知用户")
        @NotBlank(message = "取消原因不能为空")
        @Size(max = 100, message = "取消原因长度不能超过100")
        String reason
) {
}
