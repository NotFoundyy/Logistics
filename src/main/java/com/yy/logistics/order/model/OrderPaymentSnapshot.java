package com.yy.logistics.order.model;

import java.math.BigDecimal;

public record OrderPaymentSnapshot(
        Long orderId,
        Long userId,
        String orderNo,
        String waybillNo,
        String orderStatus,
        Integer payType,
        BigDecimal feeTotal,
        String remark
) {
}
