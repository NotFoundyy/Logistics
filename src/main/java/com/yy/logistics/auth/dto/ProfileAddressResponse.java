package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Address response")
public record ProfileAddressResponse(
        @Schema(description = "Address id")
        Long id,
        @Schema(description = "Contact name")
        String contactName,
        @Schema(description = "Contact phone")
        String contactPhone,
        @Schema(description = "Province")
        String province,
        @Schema(description = "City")
        String city,
        @Schema(description = "District")
        String district,
        @Schema(description = "Detail")
        String detail,
        @Schema(description = "Full address")
        String fullAddress,
        @Schema(description = "Default or not")
        Boolean isDefault
) {
}
