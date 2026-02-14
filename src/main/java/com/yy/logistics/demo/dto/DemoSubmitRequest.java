package com.yy.logistics.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "演示下单请求")
public record DemoSubmitRequest(
        @Schema(description = "寄件人姓名", example = "张三")
        @NotBlank(message = "寄件人姓名不能为空")
        @Size(max = 50, message = "寄件人姓名长度不能超过50")
        String senderName,

        @Schema(description = "寄件人手机号", example = "13800000000")
        @NotBlank(message = "寄件人手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "寄件人手机号格式不正确")
        String senderPhone,

        @Schema(description = "收件人姓名", example = "李四")
        @NotBlank(message = "收件人姓名不能为空")
        @Size(max = 50, message = "收件人姓名长度不能超过50")
        String receiverName,

        @Schema(description = "收件人手机号", example = "13900000000")
        @NotBlank(message = "收件人手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "收件人手机号格式不正确")
        String receiverPhone,

        @Schema(description = "包裹重量(kg)", example = "1.25")
        @NotNull(message = "包裹重量不能为空")
        @DecimalMin(value = "0.1", message = "包裹重量必须大于等于0.1kg")
        @DecimalMax(value = "100.0", message = "包裹重量不能超过100kg")
        BigDecimal weightKg,

        @Schema(description = "物品名称", example = "书籍")
        @NotBlank(message = "物品名称不能为空")
        @Size(max = 100, message = "物品名称长度不能超过100")
        String itemName
) {
}

