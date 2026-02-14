package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Create courier request")
public record AuthCreateCourierRequest(
        @Schema(description = "Courier name", example = "Tom")
        @NotBlank(message = "courier name cannot be blank")
        @Size(max = 50, message = "courier name max length is 50")
        String username,

        @Schema(description = "Courier phone", example = "13612345678")
        @NotBlank(message = "courier phone cannot be blank")
        @Pattern(regexp = "^1\\d{10}$", message = "courier phone format is invalid")
        String phone,

        @Schema(description = "Courier email", example = "courier@example.com")
        @Email(message = "courier email format is invalid")
        @Size(max = 100, message = "courier email max length is 100")
        String email,

        @Schema(description = "Initial password", example = "Courier123")
        @NotBlank(message = "password cannot be blank")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "password must include letters and digits, length 8-64")
        String password,

        @Schema(description = "Station id", example = "1")
        Long stationId
) {
}
