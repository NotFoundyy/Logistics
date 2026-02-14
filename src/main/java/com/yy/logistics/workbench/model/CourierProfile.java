package com.yy.logistics.workbench.model;

public record CourierProfile(
        Long courierId,
        Long userId,
        Long stationId,
        String workNo,
        String name,
        String phone
) {
}
