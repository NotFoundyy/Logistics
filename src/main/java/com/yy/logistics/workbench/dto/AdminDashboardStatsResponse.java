package com.yy.logistics.workbench.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "管理员统计面板数据")
public record AdminDashboardStatsResponse(
        @Schema(description = "订单状态分布")
        Map<String, Long> orderStatusStats,
        @Schema(description = "任务状态分布")
        Map<String, Long> taskStatusStats,
        @Schema(description = "最近7日订单趋势")
        List<AdminOrderTrendItemResponse> trend
) {
}
