package com.yy.logistics.workbench.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminOrderItem(
        Long id,
        String orderNo,
        String waybillNo,
        String status,
        Boolean refunded,
        String senderName,
        String senderPhone,
        String receiverName,
        String receiverPhone,
        BigDecimal feeTotal,
        LocalDateTime createdAt
) {
}
