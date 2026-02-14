package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "在线支付二维码信息")
public record OrderPaymentQrcodeResponse(
        @Schema(description = "订单ID", example = "1")
        Long orderId,
        @Schema(description = "订单号", example = "OD20260212194500123")
        String orderNo,
        @Schema(description = "运单号", example = "WB20260212194500123")
        String waybillNo,
        @Schema(description = "应付金额", example = "18.50")
        BigDecimal amount,
        @Schema(description = "二维码内容")
        String qrCodeText,
        @Schema(description = "二维码有效秒数", example = "300")
        Integer expireSeconds
) {
}
