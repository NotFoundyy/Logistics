package com.yy.logistics.workbench.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "工作台概览响应")
public record WorkbenchOverviewResponse(
        @Schema(description = "概览统计")
        Map<String, Long> metrics
) {
}
