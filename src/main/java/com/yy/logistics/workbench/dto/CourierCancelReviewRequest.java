package com.yy.logistics.workbench.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "快递员审核用户取消申请")
public record CourierCancelReviewRequest(
        @Schema(description = "是否同意取消", example = "true")
        @NotNull(message = "审核结果不能为空")
        Boolean approved,

        @Schema(description = "备注（拒绝时建议填写）", example = "包裹已到派送阶段，建议等待签收")
        @Size(max = 100, message = "备注长度不能超过100")
        String reason
) {
}
