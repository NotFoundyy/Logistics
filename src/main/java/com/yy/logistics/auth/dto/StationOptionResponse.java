package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Station option")
public record StationOptionResponse(
        @Schema(description = "Station id")
        Long id,
        @Schema(description = "Station name")
        String name,
        @Schema(description = "Province")
        String province,
        @Schema(description = "City")
        String city
) {
}
