package com.yy.logistics.order.model;

import java.time.LocalDateTime;

public record AcceptedTaskSnapshot(
        Long taskId,
        String waybillNo,
        String taskStatus,
        Long courierId,
        LocalDateTime acceptedAt
) {
}
