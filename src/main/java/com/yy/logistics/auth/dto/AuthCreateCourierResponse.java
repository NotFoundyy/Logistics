package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Create courier response")
public record AuthCreateCourierResponse(
        @Schema(description = "User id", example = "1")
        Long userId,
        @Schema(description = "Username")
        String username,
        @Schema(description = "Phone")
        String phone,
        @Schema(description = "Work number")
        String workNo,
        @Schema(description = "Station id")
        Long stationId,
        @Schema(description = "Roles")
        List<String> roles
) {
}
