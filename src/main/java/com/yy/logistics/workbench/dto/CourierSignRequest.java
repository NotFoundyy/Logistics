package com.yy.logistics.workbench.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "快递员签收请求")
public record CourierSignRequest(
        @Schema(description = "到付订单是否已确认收款", example = "true")
        Boolean paidConfirmed,

        @Schema(description = "签收备注（可选）", example = "收件人本人签收，现金已收")
        @Size(max = 100, message = "签收备注不能超过100字")
        String remark
) {
}
