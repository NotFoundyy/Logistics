package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Forgot password options request")
public record AuthForgotPasswordOptionsRequest(
        @Schema(description = "Account", example = "13800138000")
        @NotBlank(message = "account cannot be blank")
        @Size(max = 100, message = "account max length is 100")
        String account,

        @Schema(description = "Username", example = "yy")
        @NotBlank(message = "username cannot be blank")
        @Size(max = 50, message = "username max length is 50")
        String username
) {
}
