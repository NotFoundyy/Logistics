package com.yy.logistics.auth.controller;

import com.yy.logistics.auth.dto.AuthForgotPasswordOptionResponse;
import com.yy.logistics.auth.dto.AuthForgotPasswordOptionsRequest;
import com.yy.logistics.auth.dto.AuthForgotPasswordResetRequest;
import com.yy.logistics.auth.dto.AuthLoginRequest;
import com.yy.logistics.auth.dto.AuthLoginResponse;
import com.yy.logistics.auth.dto.AuthRegisterRequest;
import com.yy.logistics.auth.dto.AuthUserProfile;
import com.yy.logistics.auth.model.LoginUser;
import com.yy.logistics.auth.service.AuthService;
import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.enums.ErrorCode;
import com.yy.logistics.common.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Ping")
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", true);
        payload.put("time", LocalDateTime.now().toString());
        return ApiResponse.success(payload);
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.success("login success", authService.login(request));
    }

    @Operation(summary = "Register")
    @PostMapping("/register")
    public ApiResponse<AuthUserProfile> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ApiResponse.success("register success", authService.register(request));
    }

    @Operation(summary = "Forgot password options")
    @PostMapping("/forgot-password/options")
    public ApiResponse<List<AuthForgotPasswordOptionResponse>> forgotPasswordOptions(
            @Valid @RequestBody AuthForgotPasswordOptionsRequest request
    ) {
        return ApiResponse.success(authService.listForgotPasswordAddressOptions(request));
    }

    @Operation(summary = "Forgot password reset")
    @PostMapping("/forgot-password/reset")
    public ApiResponse<Void> forgotPasswordReset(@Valid @RequestBody AuthForgotPasswordResetRequest request) {
        authService.resetPasswordByForgot(request);
        return ApiResponse.success("password reset success", null);
    }

    @Operation(summary = "Current user")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ApiResponse<AuthUserProfile> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(authService.getProfile(loginUser.userId()));
    }
}
