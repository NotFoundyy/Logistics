package com.yy.logistics.workbench.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "后台订单列表项")
public record AdminOrderItemResponse(
        @Schema(description = "订单ID", example = "1")
        Long id,
        @Schema(description = "订单号")
        String orderNo,
        @Schema(description = "运单号")
        String waybillNo,
        @Schema(description = "订单状态")
        String status,
        @Schema(description = "是否已退款")
        Boolean refunded,
        @Schema(description = "寄件人")
        String senderName,
        @Schema(description = "寄件人手机号")
        String senderPhone,
        @Schema(description = "收件人")
        String receiverName,
        @Schema(description = "收件人手机号")
        String receiverPhone,
        @Schema(description = "费用总计")
        BigDecimal feeTotal,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}
