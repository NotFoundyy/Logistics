package com.yy.logistics.workbench.model;

import java.time.LocalDateTime;

public record CourierTaskItem(
        Long taskId,
        String waybillNo,
        Long orderId,
        String orderNo,
        String orderStatus,
        Boolean refunded,
        String receiverName,
        String receiverPhone,
        String receiverAddr,
        Integer payType,
        Boolean paid,
        Integer taskType,
        String taskStatus,
        LocalDateTime plannedTime,
        LocalDateTime acceptedAt
) {
}
