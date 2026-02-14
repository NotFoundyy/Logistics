package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "User profile")
public record AuthUserProfile(
        @Schema(description = "User id", example = "1")
        Long userId,
        @Schema(description = "Username")
        String username,
        @Schema(description = "Phone")
        String phone,
        @Schema(description = "Email")
        String email,
        @Schema(description = "Roles")
        List<String> roles
) {
}
