package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Forgot password reset request")
public record AuthForgotPasswordResetRequest(
        @Schema(description = "Account", example = "13800138000")
        @NotBlank(message = "account cannot be blank")
        @Size(max = 100, message = "account max length is 100")
        String account,

        @Schema(description = "Username", example = "yy")
        @NotBlank(message = "username cannot be blank")
        @Size(max = 50, message = "username max length is 50")
        String username,

        @Schema(description = "Address id", example = "1")
        @NotNull(message = "addressId cannot be null")
        Long addressId,

        @Schema(description = "New password", example = "Newpass123")
        @NotBlank(message = "new password cannot be blank")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "password must include letters and digits, length 8-64")
        String newPassword
) {
}
