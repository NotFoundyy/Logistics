package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "地图坐标点")
public record TrackingGeoPointResponse(
        @Schema(description = "纬度", example = "29.56301")
        double lat,
        @Schema(description = "经度", example = "106.55156")
        double lng
) {
}
