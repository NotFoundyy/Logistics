package com.yy.logistics.workbench.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理员订单趋势项")
public record AdminOrderTrendItemResponse(
        @Schema(description = "日期", example = "2026-02-12")
        String date,
        @Schema(description = "订单数", example = "25")
        Long count
) {
}
