package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Forgot password address option")
public record AuthForgotPasswordOptionResponse(
        @Schema(description = "Address id", example = "1")
        Long addressId,
        @Schema(description = "Option label")
        String label
) {
}
