package com.yy.logistics.auth.support;

import com.yy.logistics.auth.model.LoginUser;
import com.yy.logistics.common.enums.ErrorCode;
import com.yy.logistics.common.exception.BizException;
import org.springframework.security.core.Authentication;

public final class AuthHelper {

    private AuthHelper() {
    }

    public static LoginUser requireLoginUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }
}
