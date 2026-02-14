package com.yy.logistics.order.model;

public record TransitRouteSnapshot(
        String waybillNo,
        String senderAddr,
        String receiverAddr,
        String currentStatus,
        Integer payType
) {
}
