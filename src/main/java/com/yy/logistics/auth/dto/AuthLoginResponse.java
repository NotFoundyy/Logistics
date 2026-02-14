package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response")
public record AuthLoginResponse(
        @Schema(description = "Access token")
        String accessToken,
        @Schema(description = "Token type")
        String tokenType,
        @Schema(description = "Expire seconds")
        Long expiresIn,
        @Schema(description = "Profile")
        AuthUserProfile profile
) {
}
