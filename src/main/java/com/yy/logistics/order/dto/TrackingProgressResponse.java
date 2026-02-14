package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "运单地图进度响应")
public record TrackingProgressResponse(
        @Schema(description = "运单号")
        String waybillNo,
        @Schema(description = "当前阶段", example = "IN_TRANSIT")
        String phase,
        @Schema(description = "总进度，0~1", example = "0.52")
        double progress,
        @Schema(description = "总距离（公里）", example = "1278.5")
        double distanceKm,
        @Schema(description = "已行驶距离（公里）", example = "523.4")
        double travelledKm,
        @Schema(description = "运输速度（公里/小时）", example = "100")
        int speedKmPerHour,
        @Schema(description = "运输速度（公里/秒）", example = "0.027778")
        double speedKmPerSecond,
        @Schema(description = "已行驶秒数", example = "18852")
        long elapsedSeconds,
        @Schema(description = "总预计秒数", example = "46000")
        long totalSeconds,
        @Schema(description = "快递员接单时间")
        LocalDateTime acceptedAt,
        @Schema(description = "预计到达收货省会站时间")
        LocalDateTime arriveProvinceHubAt,
        @Schema(description = "预计到达收货地级市站时间")
        LocalDateTime arriveCityHubAt,
        @Schema(description = "预计签收时间")
        LocalDateTime signedAt,
        @Schema(description = "寄件省份")
        String senderProvince,
        @Schema(description = "寄件城市")
        String senderCity,
        @Schema(description = "寄件完整地址")
        String senderAddr,
        @Schema(description = "收件省份")
        String receiverProvince,
        @Schema(description = "收件城市")
        String receiverCity,
        @Schema(description = "收件区县")
        String receiverDistrict,
        @Schema(description = "收件完整地址")
        String receiverAddr,
        @Schema(description = "收件省会城市")
        String receiverCapitalCity,
        @Schema(description = "路径节点名称，和 route 坐标一一对应")
        List<String> routeNodeNames,
        @Schema(description = "起点坐标")
        TrackingGeoPointResponse startPoint,
        @Schema(description = "终点坐标")
        TrackingGeoPointResponse endPoint,
        @Schema(description = "当前位置坐标")
        TrackingGeoPointResponse currentPoint,
        @Schema(description = "规划路径坐标")
        List<TrackingGeoPointResponse> route
) {
}
