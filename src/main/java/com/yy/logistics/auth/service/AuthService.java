package com.yy.logistics.auth.service;

import com.yy.logistics.auth.dto.AuthCreateCourierRequest;
import com.yy.logistics.auth.dto.AuthCreateCourierResponse;
import com.yy.logistics.auth.dto.AuthForgotPasswordOptionResponse;
import com.yy.logistics.auth.dto.AuthForgotPasswordOptionsRequest;
import com.yy.logistics.auth.dto.AuthForgotPasswordResetRequest;
import com.yy.logistics.auth.dto.AuthLoginRequest;
import com.yy.logistics.auth.dto.AuthLoginResponse;
import com.yy.logistics.auth.dto.AuthRegisterRequest;
import com.yy.logistics.auth.dto.AuthUserProfile;
import com.yy.logistics.auth.dto.ProfileAddressResponse;
import com.yy.logistics.auth.dto.ProfileAddressUpsertRequest;
import com.yy.logistics.auth.dto.ProfileContactUpdateRequest;
import com.yy.logistics.auth.dto.ProfilePasswordUpdateRequest;
import com.yy.logistics.auth.dto.StationOptionResponse;
import com.yy.logistics.auth.model.AuthAddress;
import com.yy.logistics.auth.model.AuthUser;
import com.yy.logistics.auth.repository.AuthUserRepository;
import com.yy.logistics.common.enums.ErrorCode;
import com.yy.logistics.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final DateTimeFormatter WORK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        AuthUser user = authUserRepository.findByAccount(request.account())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_CREDENTIALS));

        if (user.status() == null || user.status() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }

        List<String> roles = authUserRepository.findRolesByUserId(user.id());
        String account = StringUtils.hasText(user.phone()) ? user.phone() : user.email();
        if (!StringUtils.hasText(account)) {
            account = user.username();
        }

        String token = jwtTokenService.createToken(user.id(), account, user.phone(), roles);
        AuthUserProfile profile = new AuthUserProfile(user.id(), user.username(), user.phone(), user.email(), roles);
        return new AuthLoginResponse(token, "Bearer", jwtTokenService.getExpireSeconds(), profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthUserProfile register(AuthRegisterRequest request) {
        return createUserWithRole(
                request.username(),
                request.phone(),
                request.email(),
                request.password(),
                "USER",
                "用户"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthCreateCourierResponse createCourier(AuthCreateCourierRequest request) {
        AuthUserProfile profile = createUserWithRole(
                request.username(),
                request.phone(),
                request.email(),
                request.password(),
                "COURIER",
                "快递员"
        );

        Long stationId = resolveStationId(request.stationId());
        String workNo = generateWorkNo();
        authUserRepository.insertCourier(
                profile.userId(),
                stationId,
                workNo,
                request.username().trim(),
                request.phone().trim()
        );

        return new AuthCreateCourierResponse(
                profile.userId(),
                profile.username(),
                profile.phone(),
                workNo,
                stationId,
                profile.roles()
        );
    }

    public AuthUserProfile getProfile(Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        List<String> roles = authUserRepository.findRolesByUserId(user.id());
        return new AuthUserProfile(user.id(), user.username(), user.phone(), user.email(), roles);
    }

    public List<StationOptionResponse> listStations() {
        return authUserRepository.findActiveStations();
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthUserProfile updateContact(Long userId, ProfileContactUpdateRequest request) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        String phone = request.phone().trim();
        String email = StringUtils.hasText(request.email()) ? request.email().trim() : null;

        if (authUserRepository.existsByPhoneExcludeUserId(phone, user.id())) {
            throw new BizException(ErrorCode.USER_EXISTS, "手机号已被使用");
        }
        if (StringUtils.hasText(email) && authUserRepository.existsByEmailExcludeUserId(email, user.id())) {
            throw new BizException(ErrorCode.USER_EXISTS, "邮箱已被使用");
        }

        authUserRepository.updateUserContact(user.id(), phone, email);
        return getProfile(user.id());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, ProfilePasswordUpdateRequest request) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.passwordHash())) {
            throw new BizException(ErrorCode.OLD_PASSWORD_INVALID);
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "新密码不能与原密码相同");
        }

        String passwordHash = passwordEncoder.encode(request.newPassword());
        authUserRepository.updateUserPassword(userId, passwordHash);
    }

    public List<AuthForgotPasswordOptionResponse> listForgotPasswordAddressOptions(AuthForgotPasswordOptionsRequest request) {
        AuthUser user = authUserRepository.findByAccountAndUsername(request.account().trim(), request.username().trim())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_CREDENTIALS, "账号、用户名不匹配"));

        List<AuthForgotPasswordOptionResponse> options = authUserRepository.listAddressByUserId(user.id()).stream()
                .map(address -> new AuthForgotPasswordOptionResponse(
                        address.id(),
                        address.contactName() + " / " + maskPhone(address.contactPhone()) + " / "
                                + String.join(" ", address.province(), address.city(), address.district(), address.detail())
                ))
                .toList();
        if (options.isEmpty()) {
            throw new BizException(ErrorCode.ADDRESS_NOT_FOUND, "该账号未维护常用地址，暂不支持找回");
        }
        return options;
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByForgot(AuthForgotPasswordResetRequest request) {
        AuthUser user = authUserRepository.findByAccountAndUsername(request.account().trim(), request.username().trim())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_CREDENTIALS, "账号、用户名不匹配"));

        authUserRepository.findAddressByIdAndUserId(request.addressId(), user.id())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "常用地址校验不通过"));

        if (passwordEncoder.matches(request.newPassword(), user.passwordHash())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }

        authUserRepository.updateUserPassword(user.id(), passwordEncoder.encode(request.newPassword()));
    }

    public List<ProfileAddressResponse> listAddresses(Long userId) {
        try {
            return authUserRepository.listAddressByUserId(userId).stream()
                    .map(this::mapAddress)
                    .toList();
        } catch (Exception ex) {
            log.warn("地址簿查询异常，返回空列表。userId={}", userId, ex);
            return List.of();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ProfileAddressResponse createAddress(Long userId, ProfileAddressUpsertRequest request) {
        int total = authUserRepository.countAddressByUserId(userId);
        boolean setDefault = Boolean.TRUE.equals(request.isDefault()) || total == 0;

        if (setDefault) {
            authUserRepository.clearDefaultAddressByUserId(userId);
        }

        Long addressId = authUserRepository.insertAddress(
                userId,
                request.contactName().trim(),
                request.contactPhone().trim(),
                request.province().trim(),
                request.city().trim(),
                request.district().trim(),
                request.detail().trim(),
                setDefault ? 1 : 0
        );

        return authUserRepository.findAddressByIdAndUserId(addressId, userId)
                .map(this::mapAddress)
                .orElseThrow(() -> new BizException(ErrorCode.ADDRESS_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProfileAddressResponse updateAddress(Long userId, Long addressId, ProfileAddressUpsertRequest request) {
        AuthAddress current = authUserRepository.findAddressByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.ADDRESS_NOT_FOUND));

        boolean setDefault = Boolean.TRUE.equals(request.isDefault());
        if (setDefault) {
            authUserRepository.clearDefaultAddressByUserId(userId);
        }

        authUserRepository.updateAddress(
                addressId,
                userId,
                request.contactName().trim(),
                request.contactPhone().trim(),
                request.province().trim(),
                request.city().trim(),
                request.district().trim(),
                request.detail().trim(),
                setDefault ? 1 : (current.isDefault() != null && current.isDefault() == 1 ? 1 : 0)
        );

        return authUserRepository.findAddressByIdAndUserId(addressId, userId)
                .map(this::mapAddress)
                .orElseThrow(() -> new BizException(ErrorCode.ADDRESS_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long userId, Long addressId) {
        authUserRepository.findAddressByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.ADDRESS_NOT_FOUND));
        authUserRepository.clearDefaultAddressByUserId(userId);
        authUserRepository.setDefaultAddress(userId, addressId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long userId, Long addressId) {
        AuthAddress current = authUserRepository.findAddressByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.ADDRESS_NOT_FOUND));
        authUserRepository.deleteAddressByIdAndUserId(addressId, userId);

        if (current.isDefault() != null && current.isDefault() == 1) {
            authUserRepository.findFirstAddressByUserId(userId)
                    .ifPresent(address -> {
                        authUserRepository.clearDefaultAddressByUserId(userId);
                        authUserRepository.setDefaultAddress(userId, address.id());
                    });
        }
    }

    private AuthUserProfile createUserWithRole(
            String username,
            String phone,
            String email,
            String password,
            String roleCode,
            String roleName
    ) {
        if (authUserRepository.existsByUsername(username)) {
            throw new BizException(ErrorCode.USER_EXISTS, roleName + "名称已存在");
        }
        if (authUserRepository.existsByPhone(phone)) {
            throw new BizException(ErrorCode.USER_EXISTS, "手机号已注册");
        }
        if (StringUtils.hasText(email) && authUserRepository.existsByEmail(email)) {
            throw new BizException(ErrorCode.USER_EXISTS, "邮箱已注册");
        }

        String passwordHash = passwordEncoder.encode(password);
        Long userId = authUserRepository.insertUser(username, phone, email, passwordHash);
        Long roleId = authUserRepository.findRoleIdByCode(roleCode)
                .orElseThrow(() -> new BizException(ErrorCode.SYSTEM_ERROR, "系统未初始化" + roleCode + "角色"));
        authUserRepository.insertUserRole(userId, roleId);
        return getProfile(userId);
    }

    private Long resolveStationId(Long stationId) {
        if (stationId != null) {
            return authUserRepository.findStationIdById(stationId)
                    .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "网点不存在或已停用"));
        }
        return authUserRepository.findDefaultStationId()
                .orElseThrow(() -> new BizException(ErrorCode.SYSTEM_ERROR, "未配置可用网点"));
    }

    private String generateWorkNo() {
        for (int i = 0; i < 20; i++) {
            String candidate = "CY"
                    + LocalDateTime.now().format(WORK_NO_FORMATTER)
                    + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
            if (!authUserRepository.existsCourierWorkNo(candidate)) {
                return candidate;
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "快递员工号生成失败，请稍后重试");
    }

    private ProfileAddressResponse mapAddress(AuthAddress address) {
        boolean isDefault = address.isDefault() != null && address.isDefault() == 1;
        return new ProfileAddressResponse(
                address.id(),
                address.contactName(),
                address.contactPhone(),
                address.province(),
                address.city(),
                address.district(),
                address.detail(),
                String.join(" ", address.province(), address.city(), address.district(), address.detail()),
                isDefault
        );
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "未留手机号";
        }
        String trimmed = phone.trim();
        if (trimmed.length() < 7) {
            return trimmed;
        }
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
    }
}
