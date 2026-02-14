package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Profile password update request")
public record ProfilePasswordUpdateRequest(
        @Schema(description = "Old password")
        @NotBlank(message = "oldPassword cannot be blank")
        String oldPassword,

        @Schema(description = "New password")
        @NotBlank(message = "newPassword cannot be blank")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "password must include letters and digits, length 8-64")
        String newPassword
) {
}
