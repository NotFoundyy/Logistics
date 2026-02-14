package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "订单详情")
public record OrderDetailResponse(
        @Schema(description = "订单ID", example = "1")
        Long id,
        @Schema(description = "订单号", example = "OD20260212194500123")
        String orderNo,
        @Schema(description = "运单号", example = "WB20260212194500123")
        String waybillNo,
        @Schema(description = "订单状态", example = "CREATED")
        String status,
        @Schema(description = "运单当前状态", example = "CREATED")
        String waybillStatus,
        @Schema(description = "寄件人姓名", example = "张三")
        String senderName,
        @Schema(description = "寄件人电话", example = "13800000001")
        String senderPhone,
        @Schema(description = "寄件地址")
        String senderAddr,
        @Schema(description = "收件人姓名", example = "李四")
        String receiverName,
        @Schema(description = "收件人电话", example = "13900000002")
        String receiverPhone,
        @Schema(description = "收件地址")
        String receiverAddr,
        @Schema(description = "服务类型", example = "1")
        Integer serviceType,
        @Schema(description = "支付方式", example = "1")
        Integer payType,
        @Schema(description = "是否已支付", example = "false")
        Boolean paid,
        @Schema(description = "是否已退款", example = "false")
        Boolean refunded,
        @Schema(description = "实重(kg)", example = "1.20")
        BigDecimal weight,
        @Schema(description = "体积(cm3)", example = "8000")
        BigDecimal volume,
        @Schema(description = "计费重(kg)", example = "1.50")
        BigDecimal chargeWeight,
        @Schema(description = "总费用", example = "19.00")
        BigDecimal feeTotal,
        @Schema(description = "备注")
        String remark,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}
