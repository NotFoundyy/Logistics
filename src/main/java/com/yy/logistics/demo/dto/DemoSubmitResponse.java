package com.yy.logistics.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "演示下单响应")
public record DemoSubmitResponse(
        @Schema(description = "订单号", example = "OD20260212193000")
        String orderNo,
        @Schema(description = "运单号", example = "WB20260212193000")
        String waybillNo,
        @Schema(description = "当前状态", example = "CREATED")
        String status
) {
}

