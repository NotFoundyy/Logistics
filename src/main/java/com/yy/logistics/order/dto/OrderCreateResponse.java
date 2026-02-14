package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "创建订单响应")
public record OrderCreateResponse(
        @Schema(description = "订单ID", example = "1")
        Long orderId,
        @Schema(description = "订单号", example = "OD20260212194500123")
        String orderNo,
        @Schema(description = "运单号", example = "WB20260212194500123")
        String waybillNo,
        @Schema(description = "订单状态", example = "CREATED")
        String status,
        @Schema(description = "费用总计", example = "19.00")
        BigDecimal feeTotal,
        @Schema(description = "支付方式：1在线，2到付", example = "1")
        Integer payType,
        @Schema(description = "是否已支付", example = "false")
        Boolean paid
) {
}
