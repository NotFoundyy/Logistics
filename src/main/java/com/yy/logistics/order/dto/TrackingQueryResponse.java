package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "运单轨迹查询响应")
public record TrackingQueryResponse(
        @Schema(description = "运单号", example = "WB20260212194500123")
        String waybillNo,
        @Schema(description = "当前状态", example = "CREATED")
        String currentStatus,
        @Schema(description = "轨迹节点列表")
        List<TrackingEventResponse> events
) {
}
