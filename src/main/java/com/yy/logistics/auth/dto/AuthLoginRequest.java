package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Login request")
public record AuthLoginRequest(
        @Schema(description = "Account", example = "admin")
        @NotBlank(message = "account cannot be blank")
        @Size(max = 100, message = "account max length is 100")
        String account,

        @Schema(description = "Password", example = "admin")
        @NotBlank(message = "password cannot be blank")
        @Size(min = 4, max = 64, message = "password length must be between 4 and 64")
        String password
) {
}
