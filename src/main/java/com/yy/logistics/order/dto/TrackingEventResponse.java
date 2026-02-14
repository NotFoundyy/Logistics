package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "轨迹节点")
public record TrackingEventResponse(
        @Schema(description = "节点时间")
        LocalDateTime eventTime,
        @Schema(description = "节点类型", example = "CREATED")
        String eventType,
        @Schema(description = "网点ID")
        Long stationId,
        @Schema(description = "快递员ID")
        Long courierId,
        @Schema(description = "节点描述")
        String description
) {
}
