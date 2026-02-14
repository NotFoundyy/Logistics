package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "修改收件信息响应")
public record OrderReceiverUpdateResponse(
        @Schema(description = "订单号", example = "OD20260212194500123")
        String orderNo,
        @Schema(description = "运单号", example = "WB20260212194500123")
        String waybillNo,
        @Schema(description = "调整前费用", example = "18.50")
        BigDecimal oldFeeTotal,
        @Schema(description = "调整后费用", example = "22.50")
        BigDecimal newFeeTotal,
        @Schema(description = "费用差额", example = "4.00")
        BigDecimal feeDelta
) {
}
