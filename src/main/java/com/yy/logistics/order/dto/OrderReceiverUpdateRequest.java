package com.yy.logistics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "修改收件信息请求")
public record OrderReceiverUpdateRequest(
        @Schema(description = "收件人姓名", example = "李四")
        @NotBlank(message = "收件人姓名不能为空")
        @Size(max = 50, message = "收件人姓名长度不能超过50")
        String receiverName,

        @Schema(description = "收件人手机号", example = "13900000002")
        @NotBlank(message = "收件人手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "收件人手机号格式不正确")
        String receiverPhone,

        @Schema(description = "收件地址", example = "广东省 广州市 天河区 珠江新城88号")
        @NotBlank(message = "收件地址不能为空")
        @Size(max = 255, message = "收件地址长度不能超过255")
        String receiverAddr,

        @Schema(description = "修改原因", example = "收件人临时改地址")
        @Size(max = 100, message = "修改原因长度不能超过100")
        String reason
) {
}
