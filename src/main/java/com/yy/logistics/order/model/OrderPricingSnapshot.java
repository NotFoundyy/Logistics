package com.yy.logistics.order.model;

import java.math.BigDecimal;

public record OrderPricingSnapshot(
        Long orderId,
        Long userId,
        String orderNo,
        String waybillNo,
        String orderStatus,
        String waybillStatus,
        String senderAddr,
        String receiverAddr,
        Integer serviceType,
        BigDecimal weight,
        BigDecimal volume,
        BigDecimal insuredAmount,
        BigDecimal chargeWeight,
        BigDecimal feeTotal
) {
}
