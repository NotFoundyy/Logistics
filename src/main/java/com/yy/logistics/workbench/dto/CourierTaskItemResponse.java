package com.yy.logistics.workbench.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "快递员任务列表项")
public record CourierTaskItemResponse(
        @Schema(description = "任务ID", example = "1")
        Long taskId,
        @Schema(description = "运单号")
        String waybillNo,
        @Schema(description = "订单ID")
        Long orderId,
        @Schema(description = "订单号")
        String orderNo,
        @Schema(description = "订单状态")
        String orderStatus,
        @Schema(description = "是否已退款")
        Boolean refunded,
        @Schema(description = "收件人")
        String receiverName,
        @Schema(description = "收件人手机号")
        String receiverPhone,
        @Schema(description = "收件地址")
        String receiverAddr,
        @Schema(description = "支付方式：1在线，2到付")
        Integer payType,
        @Schema(description = "是否已支付")
        Boolean paid,
        @Schema(description = "任务类型：1揽收，2派送")
        Integer taskType,
        @Schema(description = "任务状态")
        String taskStatus,
        @Schema(description = "计划时间")
        LocalDateTime plannedTime,
        @Schema(description = "接单时间")
        LocalDateTime acceptedAt
) {
}
