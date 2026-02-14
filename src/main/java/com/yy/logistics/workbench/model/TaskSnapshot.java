package com.yy.logistics.workbench.model;

public record TaskSnapshot(
        Long taskId,
        Long orderId,
        String waybillNo,
        String orderStatus,
        Integer payType,
        Integer taskType,
        String status,
        Long courierId
) {
}
