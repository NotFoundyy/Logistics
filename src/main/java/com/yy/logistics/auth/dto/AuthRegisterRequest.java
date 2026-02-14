package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Register request")
public record AuthRegisterRequest(
        @Schema(description = "Username", example = "yy")
        @NotBlank(message = "username cannot be blank")
        @Size(max = 50, message = "username max length is 50")
        String username,

        @Schema(description = "Phone", example = "13800138000")
        @NotBlank(message = "phone cannot be blank")
        @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
        String phone,

        @Schema(description = "Email", example = "yy@example.com")
        @Email(message = "email format is invalid")
        @Size(max = 100, message = "email max length is 100")
        String email,

        @Schema(description = "Password", example = "Pass1234")
        @NotBlank(message = "password cannot be blank")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "password must include letters and digits, length 8-64")
        String password
) {
}
