package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "我的订单列表项")
public record OrderListItemResponse(
        @Schema(description = "订单ID", example = "1")
        Long id,
        @Schema(description = "订单号", example = "OD20260212194500123")
        String orderNo,
        @Schema(description = "运单号", example = "WB20260212194500123")
        String waybillNo,
        @Schema(description = "关系：SENDER(发件)/RECEIVER(收件)", example = "SENDER")
        String relationType,
        @Schema(description = "订单状态", example = "CREATED")
        String status,
        @Schema(description = "支付方式：1在线支付，2到付", example = "1")
        Integer payType,
        @Schema(description = "是否已支付", example = "false")
        Boolean paid,
        @Schema(description = "是否已退款", example = "false")
        Boolean refunded,
        @Schema(description = "费用总计", example = "19.00")
        BigDecimal feeTotal,
        @Schema(description = "收件人姓名", example = "李四")
        String receiverName,
        @Schema(description = "收件人电话", example = "13900000002")
        String receiverPhone,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}
