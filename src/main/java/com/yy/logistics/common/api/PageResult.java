package com.yy.logistics.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页响应体")
public record PageResult<T>(
        @Schema(description = "当前页码（从1开始）", example = "1")
        int page,
        @Schema(description = "每页条数", example = "10")
        int size,
        @Schema(description = "总记录数", example = "100")
        long total,
        @Schema(description = "数据列表")
        List<T> records
) {
}
