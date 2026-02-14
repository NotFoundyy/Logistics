package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Address upsert request")
public record ProfileAddressUpsertRequest(
        @Schema(description = "Contact name")
        @NotBlank(message = "contactName cannot be blank")
        @Size(max = 50, message = "contactName max length is 50")
        String contactName,

        @Schema(description = "Contact phone")
        @NotBlank(message = "contactPhone cannot be blank")
        @Pattern(regexp = "^1\\d{10}$", message = "contactPhone format is invalid")
        String contactPhone,

        @Schema(description = "Province")
        @NotBlank(message = "province cannot be blank")
        String province,

        @Schema(description = "City")
        @NotBlank(message = "city cannot be blank")
        String city,

        @Schema(description = "District")
        @NotBlank(message = "district cannot be blank")
        String district,

        @Schema(description = "Detail")
        @NotBlank(message = "detail cannot be blank")
        @Size(max = 255, message = "detail max length is 255")
        String detail,

        @Schema(description = "Default or not")
        Boolean isDefault
) {
}
