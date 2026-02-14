package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "运费试算响应")
public record PricingQuoteResponse(
        @Schema(description = "实重(kg)", example = "1.20")
        BigDecimal actualWeight,
        @Schema(description = "体积重(kg)", example = "1.33")
        BigDecimal volumeWeight,
        @Schema(description = "计费重(kg)", example = "1.50")
        BigDecimal chargeWeight,
        @Schema(description = "首重费用", example = "12.00")
        BigDecimal baseFee,
        @Schema(description = "续重费用", example = "2.00")
        BigDecimal continueFee,
        @Schema(description = "服务附加费", example = "0.00")
        BigDecimal serviceFee,
        @Schema(description = "偏远地区附加费", example = "0.00")
        BigDecimal remoteFee,
        @Schema(description = "保价费用", example = "5.00")
        BigDecimal insuredFee,
        @Schema(description = "总费用", example = "19.00")
        BigDecimal totalFee,
        @Schema(description = "计费规则说明")
        String ruleDesc
) {
}
