package com.yy.logistics.auth.controller;

import com.yy.logistics.auth.dto.AuthUserProfile;
import com.yy.logistics.auth.dto.ProfileAddressResponse;
import com.yy.logistics.auth.dto.ProfileAddressUpsertRequest;
import com.yy.logistics.auth.dto.ProfileContactUpdateRequest;
import com.yy.logistics.auth.dto.ProfilePasswordUpdateRequest;
import com.yy.logistics.auth.model.LoginUser;
import com.yy.logistics.auth.service.AuthService;
import com.yy.logistics.auth.support.AuthHelper;
import com.yy.logistics.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "个人中心接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class ProfileController {

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "获取个人资料")
    @GetMapping("/me")
    public ApiResponse<AuthUserProfile> profile(Authentication authentication) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success(authService.getProfile(loginUser.userId()));
    }

    @Operation(summary = "修改手机号/邮箱")
    @PutMapping("/contact")
    public ApiResponse<AuthUserProfile> updateContact(
            Authentication authentication,
            @Valid @RequestBody ProfileContactUpdateRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success("更新成功", authService.updateContact(loginUser.userId(), request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(
            Authentication authentication,
            @Valid @RequestBody ProfilePasswordUpdateRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        authService.updatePassword(loginUser.userId(), request);
        return ApiResponse.success("密码修改成功", null);
    }

    @Operation(summary = "地址簿列表")
    @GetMapping("/addresses")
    public ApiResponse<List<ProfileAddressResponse>> listAddresses(Authentication authentication) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success(authService.listAddresses(loginUser.userId()));
    }

    @Operation(summary = "新增地址")
    @PostMapping("/addresses")
    public ApiResponse<ProfileAddressResponse> createAddress(
            Authentication authentication,
            @Valid @RequestBody ProfileAddressUpsertRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success("新增成功", authService.createAddress(loginUser.userId(), request));
    }

    @Operation(summary = "编辑地址")
    @PutMapping("/addresses/{addressId}")
    public ApiResponse<ProfileAddressResponse> updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @Valid @RequestBody ProfileAddressUpsertRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success("更新成功", authService.updateAddress(loginUser.userId(), addressId, request));
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/addresses/{addressId}/default")
    public ApiResponse<Void> setDefaultAddress(Authentication authentication, @PathVariable Long addressId) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        authService.setDefaultAddress(loginUser.userId(), addressId);
        return ApiResponse.success("设置成功", null);
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(Authentication authentication, @PathVariable Long addressId) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        authService.deleteAddress(loginUser.userId(), addressId);
        return ApiResponse.success("删除成功", null);
    }
}
