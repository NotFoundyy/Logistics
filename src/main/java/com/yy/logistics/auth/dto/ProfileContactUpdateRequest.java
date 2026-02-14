package com.yy.logistics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Profile contact update request")
public record ProfileContactUpdateRequest(
        @Schema(description = "Phone")
        @NotBlank(message = "phone cannot be blank")
        @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
        String phone,

        @Schema(description = "Email")
        @Email(message = "email format is invalid")
        @Size(max = 100, message = "email max length is 100")
        String email
) {
}
