package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "创建订单请求")
public record OrderCreateRequest(
        @Schema(description = "寄件人姓名", example = "张三")
        @NotBlank(message = "寄件人姓名不能为空")
        @Size(max = 50, message = "寄件人姓名长度不能超过50")
        String senderName,

        @Schema(description = "寄件人手机号", example = "13800000001")
        @NotBlank(message = "寄件人手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "寄件人手机号格式不正确")
        String senderPhone,

        @Schema(description = "寄件地址", example = "重庆市渝北区龙塔街道xx号")
        @NotBlank(message = "寄件地址不能为空")
        @Size(max = 255, message = "寄件地址长度不能超过255")
        String senderAddr,

        @Schema(description = "收件人姓名", example = "李四")
        @NotBlank(message = "收件人姓名不能为空")
        @Size(max = 50, message = "收件人姓名长度不能超过50")
        String receiverName,

        @Schema(description = "收件人手机号", example = "13900000002")
        @NotBlank(message = "收件人手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "收件人手机号格式不正确")
        String receiverPhone,

        @Schema(description = "收件地址", example = "上海市浦东新区世纪大道xx号")
        @NotBlank(message = "收件地址不能为空")
        @Size(max = 255, message = "收件地址长度不能超过255")
        String receiverAddr,

        @Schema(description = "服务类型：1标准，2加急", example = "1")
        @NotNull(message = "服务类型不能为空")
        @Min(value = 1, message = "服务类型不合法")
        @Max(value = 2, message = "服务类型不合法")
        Integer serviceType,

        @Schema(description = "支付方式：1在线，2到付", example = "1")
        @NotNull(message = "支付方式不能为空")
        @Min(value = 1, message = "支付方式不合法")
        @Max(value = 2, message = "支付方式不合法")
        Integer payType,

        @Schema(description = "实重(kg)", example = "1.2")
        @NotNull(message = "实重不能为空")
        @DecimalMin(value = "0.01", message = "实重必须大于0")
        BigDecimal weight,

        @Schema(description = "体积(cm3)", example = "8000")
        @NotNull(message = "体积不能为空")
        @DecimalMin(value = "0", message = "体积不能小于0")
        BigDecimal volume,

        @Schema(description = "保价金额", example = "1000")
        @NotNull(message = "保价金额不能为空")
        @DecimalMin(value = "0", message = "保价金额不能小于0")
        BigDecimal insuredAmount,

        @Schema(description = "备注", example = "易碎品请轻拿轻放")
        @Size(max = 255, message = "备注长度不能超过255")
        String remark
) {
}
